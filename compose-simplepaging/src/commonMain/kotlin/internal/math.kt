package net.lsafer.compose.simplepaging.internal

@PublishedApi
internal fun ULong.ceilDiv(other: UInt): ULong = (this + other - 1u) / other
