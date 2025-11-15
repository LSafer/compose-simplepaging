package net.lsafer.compose.simplepaging

import kotlinx.serialization.Serializable

/**
 * Represents a reference to a page in pagination,
 * supporting both cursor-based and offset-based paging.
 */
@Serializable
data class PageRef(
    val cursor: String? = null,
    val offset: ULong? = null,
)

/**
 * Represents a query for a paginated list, including
 * search parameters, page size, and page reference.
 */
@Serializable
data class PageQuery<S>(
    val search: S? = null,
    val pageSize: UInt = 24u,
    val ref: PageRef = PageRef(),
)

/**
 * Represents the result of a paginated fetch, including
 * items, the next item count (or null if unknown), and
 * next page reference.
 */
@Serializable
data class PageResult<T>(
    val items: List<T> = emptyList(),
    val nextItemCount: ULong? = null,
    val nextRef: PageRef = PageRef(),
)

/**
 * An immutable object containing a particular state of a paging class.
 *
 * @param query the current targeted page by the user.
 * @param result the result of the last fetch.
 * @param isStale true, indicating that [result] didn't result from [query].
 */
@Serializable
data class PagingState<T, S>(
    val query: PageQuery<S> = PageQuery(),
    val result: PageResult<T> = PageResult(),
    val isStale: Boolean = false,
)

/**
 * Type alias for a suspend function that fetches
 * a page of items given a query or returns null
 * on failure.
 */
typealias PagingFetcher<T, S> = suspend (PageQuery<S>) -> PageResult<T>?
