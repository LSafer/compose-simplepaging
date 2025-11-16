package net.lsafer.compose.simplepaging

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.lsafer.compose.simplepaging.internal.throwIfFatal

/**
 * Manages incremental "chunk" loading for lists, using a simple query +
 * reference model.
 *
 * Unlike [Paging], [Chunking] does not derive staleness from query changes
 * nor manage internal state transitions. Each call to [fetch] or [fetchMore]
 * directly defines the next state.
 *
 * [Chunking] also differs from [Paging] in its concurrency model: only one
 * [fetch] operation can run at a time, enforced by a non-reentrant [Mutex].
 * Back-to-back calls to [fetch] or [fetchMore] will not run concurrently,
 * but will queue sequentially unless the caller debounce or cancel them.
 *
 * State is exposed via [state] and items via [items]. All updates replace
 * the old state with a new [ChunkingState] snapshot.
 *
 * Data loading is done via the provided [fetcher]. Use [fetch] to load the
 * first chunk for a search value, and [fetchMore] to append additional items.
 *
 * Updates are thread-safe and [isLoading] tracks in-progress operations.
 *
 * Errors from fetch operations are collected in [errors].
 *
 * ### HOW TO USE
 *
 * The user is responsible for **calling [fetch] and [fetchMore] at the
 * appropriate time**:
 *  - Call [fetch] whenever the search value changes or a manual refresh is
 *    requested. This clears existing items and resets pagination.
 *  - Call [fetchMore] when additional chunks should be loaded (e.g., as the
 *    user scrolls).
 *
 * Recommended usage pattern:
 * 1. Store the search in a reactive state.
 * 2. Debounce or filter changes if needed.
 * 3. Call [fetch] whenever the search changes or a refresh is desired.
 * 4. Use the Boolean result if you need to know whether the caller should retry.
 * 5. Optionally, wrap the [fetch] call with a timeout to prevent fetch queueing.
 */
class Chunking<T, S>(private val fetcher: Fetcher<T, S>) {
    /**
     * Type alias for a suspend function that fetches
     * a page of items given a query or returns null
     * on failure.
     */
    typealias Fetcher<T, S> = suspend (PageQuery<S>) -> PageResult<T>?

    private val mutex = Mutex()

    var state by mutableStateOf(ChunkingState<S>())
        private set

    private val _items = mutableStateListOf<T>()
    val items get() = _items.toList()

    var isLoading by mutableStateOf(false)
        private set

    val errors = mutableStateListOf<Throwable>()

    /**
     * Loads the first chunk of items for the given [newSearch] value.
     *
     * NOTE: the functions [fetch] and [fetchMore] are guarded by a single non-reentrant [Mutex].
     *
     * When [fetch] is called, the current search will be changed
     * to [newSearch] regardless if the function succeeded or failed.
     *
     * When successful, clears the current [items] and replaces
     * them with the resultant items.
     *
     * @param newSearch the search value to fetch items for.
     * @param limit the maximum number of items to fetch in this chunk.
     * @return true if the fetch succeeded; false if the fetch failed.
     */
    suspend fun fetch(newSearch: S? = state.search, limit: UInt = 24u): Boolean = mutex.withLock {
        val fetchQuery = PageQuery(
            search = newSearch,
            pageSize = limit,
            ref = PageRef(),
        )

        this.state = ChunkingState(
            search = fetchQuery.search,
            nextItemCount = null,
            nextRef = fetchQuery.ref,
            isStale = true,
        )

        val fetchResult = try {
            isLoading = true
            fetcher(fetchQuery) ?: return false
        } catch (e: Throwable) {
            e.throwIfFatal()
            errors += e
            return false
        } finally {
            isLoading = false
        }

        this._items.clear()
        this._items += fetchResult.items
        this.state = ChunkingState(
            search = fetchQuery.search,
            nextItemCount = fetchResult.nextItemCount,
            nextRef = fetchResult.nextRef,
            isStale = false,
        )

        return true
    }

    /**
     * Loads the next chunk of items based on the current [state].
     *
     * NOTE: the functions [fetch] and [fetchMore] are guarded by a single non-reentrant [Mutex].
     *
     * When successful, clears the current [items], if stale, and appends
     * the resultant items.
     *
     * @param limit the maximum number of items to fetch in this chunk.
     * @return true if the fetch succeeded; false if the fetch failed.
     */
    suspend fun fetchMore(limit: UInt = 24u): Boolean = mutex.withLock {
        val wasStale = this.state.isStale
        val fetchQuery = PageQuery(
            search = this.state.search,
            pageSize = limit,
            ref = this.state.nextRef,
        )

        val fetchResult = try {
            isLoading = true
            fetcher(fetchQuery) ?: return false
        } catch (e: Throwable) {
            e.throwIfFatal()
            errors += e
            return false
        } finally {
            isLoading = false
        }

        if (wasStale)
            this._items.clear()
        this._items += fetchResult.items
        this.state = ChunkingState(
            search = fetchQuery.search,
            nextItemCount = fetchResult.nextItemCount,
            nextRef = fetchResult.nextRef,
            isStale = false,
        )

        return true
    }
}
