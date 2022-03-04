package de.ferdoran.sro.net

import io.ktor.utils.io.pool.*

object Constants {
    const val BUFFER_SIZE = 4096
    private const val BUFFER_POOL_SIZE = 1024
    val BUFFER_POOL = object: DefaultPool<ByteArray>(BUFFER_POOL_SIZE) {
        override fun produceInstance() = ByteArray(BUFFER_SIZE)
    }
}