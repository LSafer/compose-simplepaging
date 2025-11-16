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
