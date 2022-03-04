package de.ferdoran.sro.net

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
class Server(private val host: String = "0.0.0.0", private val port: Int = 15000) {
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val log = LoggerFactory.getLogger(this::class.java)
    private val sessions = ConcurrentHashMap<UUID, SessionChannels>()
    private val scope = CoroutineScope(SupervisorJob())
    private val disconnectedSessionChannel = Channel<UUID>()
    private var serverSocket: ServerSocket? = null
    val packetChannel = Channel<Pair<UUID, ByteArray>>(capacity = Constants.BUFFER_SIZE * 128 * 2000)

    fun start() = runBlocking {
        serverSocket = aSocket(selectorManager).tcp().bind(host, port)
        scope.launch {
            log.info("started listening on $host:$port")
            while (isActive) {
                try {
                    supervisorScope {
                        val socket = serverSocket!!.accept()
                        val session = Session(UUID.randomUUID(), socket)
                        log.debug("new connection from ${socket.remoteAddress.toJavaAddress().hostname}")
                        sessions[session.id] = session.start(disconnectedSessionChannel, packetChannel)
                    }
                } catch (ex: Exception) {
                    // should we shut down here?
                    log.error("failed to accept connection: ${ex.message}", ex)
                }
            }
        }
        scope.launch {
            disconnectedSessionChannel.consumeEach {
                sessions.remove(it)
                log.debug("removed session $it")
            }
        }
    }

    fun stop(reason: String? = "unknown") {
        val msg = "stopping server with reason: $reason"
        log.info(msg)
        scope.cancel()
        serverSocket?.close()
        sessions.forEach {
                it.value.second.trySend(msg)
        }
        disconnectedSessionChannel.close()
        log.info("stopped server successfully")
    }
}

