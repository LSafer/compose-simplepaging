@file:JvmMultifileClass
@file:JvmName("ExtensionsKt")

package net.lsafer.compose.simplepaging

import net.lsafer.compose.simplepaging.internal.ceilDiv
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

// ========== query.search ========== //

inline val <S> Paging<*, S>.search: S?
    get() = state.query.search

/**
 * Updates the search parameters to a new value and resets the page reference.
 */
fun <S> Paging<*, S>.editSearch(newSearch: S?) = editSearch { newSearch }

/**
 * Transforms the current search parameters and resets the page reference.
 */
fun <S> Paging<*, S>.editSearch(transform: (S?) -> S?) =
    editQuery { it.query.copy(search = transform(it.query.search), ref = PageRef()) }

// ========== query.pageSize ========== //

inline val Paging<*, *>.pageSize: UInt
    get() = state.query.pageSize

/**
 * Sets a new page size and resets the page reference.
 */
fun Paging<*, *>.editPageSize(newPageSize: UInt) = editPageSize { newPageSize }

/**
 * Transforms the current page size and resets the page reference.
 */
fun Paging<*, *>.editPageSize(transform: (UInt) -> UInt) =
    editQuery { it.query.copy(pageSize = transform(it.query.pageSize), ref = PageRef()) }

// ========== query.ref ========== //

/**
 * Advances the paging state to the next page based on the current result's nextRef.
 */
fun Paging<*, *>.advance() =
    editQuery { it.query.copy(ref = it.result.nextRef) }

/**
 * Advances the paging state to a specific page reference.
 */
fun Paging<*, *>.advanceTo(pageRef: PageRef) =
    editQuery { it.query.copy(ref = pageRef) }

/**
 * Advances the paging state to a page with a specific cursor and/or offset.
 */
fun Paging<*, *>.advanceTo(cursor: String? = null, offset: ULong? = null) =
    editQuery { it.query.copy(ref = PageRef(cursor, offset)) }

// ========== STATE ========== //

val Paging<*, *>.isLoadingOrStale: Boolean
    get() = isLoading || state.isStale

// ========== RESULT ========== //

inline val <T> Paging<T, *>.items: List<T>
    get() = state.result.items

inline val Paging<*, *>.nextItemCount: ULong?
    get() = state.result.nextItemCount

inline val Paging<*, *>.nextOffset: ULong?
    get() = state.result.nextRef.offset

//

inline val Paging<*, *>.hasMore: Boolean
    get() = nextItemCount != 0uL

inline val Paging<*, *>.currentOffset: ULong?
    get() {
        val nextOffset = nextOffset ?: return null
        return nextOffset - items.size.toUInt()
    }

inline val Paging<*, *>.totalItemCount: ULong?
    get() {
        val nextOffset = nextOffset ?: return null
        val nextItemCount = nextItemCount ?: return null
        return nextOffset + nextItemCount
    }

// ========== NAVIGATION ========== //

inline val Paging<*, *>.pageOrdinal: UInt?
    get() {
        val currentOffset = currentOffset ?: return null
        return (currentOffset / pageSize).toUInt()
    }

inline val Paging<*, *>.pageNumber: UInt?
    get() {
        val pageOrdinal = pageOrdinal ?: return null
        return pageOrdinal + 1u
    }

inline val Paging<*, *>.nextPageCount: UInt?
    get() {
        val nextItemCount = nextItemCount ?: return null
        return nextItemCount.ceilDiv(pageSize).toUInt()
    }

inline val Paging<*, *>.totalPageCount: UInt?
    get() {
        val totalItemCount = totalItemCount ?: return null
        return totalItemCount.ceilDiv(pageSize).toUInt()
    }
