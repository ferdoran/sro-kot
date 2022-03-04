package de.ferdoran.sro

import de.ferdoran.sro.plugins.GatewayServer
import de.ferdoran.sro.plugins.configureMonitoring
import de.ferdoran.sro.plugins.configureRouting
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import sun.misc.Signal
import kotlin.system.exitProcess

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {
    launch {
        embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
            configureRouting()
            configureMonitoring()
        }.start(wait = false)
    }

    launch {
        val log = LoggerFactory.getLogger("GatewayServer")
        launch {
            GatewayServer.start()
        }
        launch {
            GatewayServer.packetChannel.consumeEach {
                log.debug("received message from session ${it.first}: ${String(it.second, Charsets.US_ASCII)}")
            }
        }
    }

    Signal.handle(Signal("INT")) {
        GatewayServer.stop("received ${it.name} signal")
        exitProcess(0)
    }

}
