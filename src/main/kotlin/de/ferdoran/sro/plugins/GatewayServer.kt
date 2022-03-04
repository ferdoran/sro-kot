package de.ferdoran.sro.plugins

import de.ferdoran.sro.net.Server
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
val GatewayServer = Server(port = 15779)
