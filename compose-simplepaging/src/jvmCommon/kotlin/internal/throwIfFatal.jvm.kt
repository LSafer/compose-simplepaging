package net.lsafer.compose.simplepaging.internal

import kotlin.coroutines.cancellation.CancellationException

@Suppress("removal")
internal actual fun Throwable.throwIfFatal() {
    // taken from arrow.kt
    when (this) {
        is VirtualMachineError,
        is ThreadDeath,
        is InterruptedException,
        is LinkageError,
        is CancellationException,
        -> throw this
    }
}
