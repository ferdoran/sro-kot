package de.ferdoran.sro.net

import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*

@ExperimentalUnsignedTypes
typealias SessionChannels = Pair<SendChannel<ByteBuffer>, SendChannel<String>>

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
class Session(val id: UUID, private val socket: Socket) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob())
    private val outgoingPacketChannel = Channel<ByteBuffer>(capacity = 1024)
    private val interruptChannel = Channel<String>()

    fun start(
        disconnectedSessionChannel: Channel<UUID>,
        incomingPacketChannel: Channel<Pair<UUID, ByteArray>>
    ): SessionChannels {

        scope.launch {
            val interruptMsg = interruptChannel.receive()
            log.debug("interrupted session with reason: $interruptMsg")
            disconnectedSessionChannel.send(id)
            stop(interruptMsg)
        }

        scope.launch {
            val sockedReadChannel = socket.openReadChannel()
            while (true) {
                val buffer = Constants.BUFFER_POOL.borrow()
                val bytesRead = sockedReadChannel.readAvailable(buffer)
                if (bytesRead == -1) {
                    // close?
                    disconnectedSessionChannel.send(id)
                    stop("failed to read data from socket")
                    break
                }
                incomingPacketChannel.send(Pair(id, buffer.copyOf()))
                Constants.BUFFER_POOL.recycle(buffer)
            }
        }
        scope.launch {
            val socketWriteChannel = socket.openWriteChannel(autoFlush = true)
            outgoingPacketChannel.consumeEach {
                try {
                    socketWriteChannel.writeFully(it)
                } catch (ex: Exception) {
                    log.error("failed to write data to socket: ${ex.message}", ex)
                    stop("failed to write data to socket")
                }
            }
        }

        return SessionChannels(outgoingPacketChannel, interruptChannel)
    }

    private fun stop(msg: String = "unknown") {
        scope.cancel()
        socket.close()
        log.debug("closed session $id with reason: $msg")
    }
}