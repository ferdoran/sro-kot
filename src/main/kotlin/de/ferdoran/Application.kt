package de.ferdoran

import io.ktor.server.engine.*
import io.ktor.server.cio.*
import de.ferdoran.plugins.*

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        configureSockets()
        configureRouting()
        configureMonitoring()
    }.start(wait = true)
}
