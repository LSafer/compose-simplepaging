@file:JvmMultifileClass
@file:JvmName("ExtensionsKt")

package net.lsafer.compose.simplepaging

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// ========== STATE ========== //

val Chunking<*, *>.isLoadingOrStale: Boolean
    get() = isLoading || state.isStale

// ========== RESULT ========== //

inline val Chunking<*, *>.nextItemCount: ULong?
    get() = state.nextItemCount

inline val Chunking<*, *>.nextOffset: ULong?
    get() = state.nextRef.offset

//

inline val Chunking<*, *>.hasMore: Boolean
    get() = nextItemCount != 0uL

inline val Chunking<*, *>.currentOffset: ULong?
    get() {
        val nextOffset = nextOffset ?: return null
        return nextOffset - items.size.toUInt()
    }

inline val Chunking<*, *>.totalItemCount: ULong?
    get() {
        val nextOffset = nextOffset ?: return null
        val nextItemCount = nextItemCount ?: return null
        return nextOffset + nextItemCount
    }
