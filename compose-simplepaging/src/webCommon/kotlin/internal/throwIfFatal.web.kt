package net.lsafer.compose.simplepaging.internal

import kotlin.coroutines.cancellation.CancellationException

internal actual fun Throwable.throwIfFatal() {
    // taken from arrow.kt
    when (this) {
        is CancellationException
        -> throw this
    }
}
