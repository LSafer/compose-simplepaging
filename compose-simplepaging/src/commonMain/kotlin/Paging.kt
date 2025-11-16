package net.lsafer.compose.simplepaging

import androidx.compose.runtime.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.Serializable
import net.lsafer.compose.simplepaging.internal.throwIfFatal

/**
 * Manages the state of a paginated list, including query, results, loading
 * state, and errors.
 *
 * State is immutable and exposed via [state]. All updates  to the query or
 * results produce a new [State] snapshot.
 *
 * Fetching is done via the provided [fetcher]. Call [fetch] to load data
 * for the current or transformed query.
 *
 * Updates are thread-safe and [isLoading] tracks in-progress fetches.
 *
 * The [editQuery], [editSearch], [editPageSize], and [advance] functions
 * allow modifying the query while optionally marking the state as stale
 * until fetched.
 *
 * Errors from fetch operations are collected in [errors].
 *
 * ### HOW TO USE
 *
 * The user is responsible for **calling [fetch] at the appropriate time**:
 *  - When `state.query.isStale` is true to refresh after a query change.
 *  - On-demand, for example to refresh or retry failed fetches.
 *
 * Recommended usage pattern:
 * 1. Observe `state.query` (e.g., via [snapshotFlow] in Compose).
 * 2. Debounce or filter changes if needed.
 * 3. Call [fetch] whenever the query changes or a refresh is desired.
 * 4. Use the Boolean result if you need to know whether the caller should retry.
 */
class Paging<T, S>(private val fetcher: Fetcher<T, S>) {
    /**
     * Type alias for a suspend function that fetches
     * a page of items given a query or returns null
     * on failure.
     */
    typealias Fetcher<T, S> = suspend (PageQuery<S>) -> PageResult<T>?

    /**
     * An immutable object containing a particular paging state.
     *
     * @param query the current targeted page by the user.
     * @param result the result of the last fetch.
     * @param isStale true, indicating that [result] didn't result from [query].
     */
    @Serializable
    data class State<T, S>(
        val query: PageQuery<S> = PageQuery(),
        val result: PageResult<T> = PageResult(),
        val isStale: Boolean = false,
    )

    private val lock = SynchronizedObject()

    var state by mutableStateOf(State<T, S>())
        private set

    private var _isLoading by mutableStateOf(0)
    val isLoading by derivedStateOf { _isLoading > 0 }

    val errors = mutableStateListOf<Throwable>()

    /**
     * Atomically updates the current query to a new one.
     */
    fun editQuery(newQuery: PageQuery<S>) = editQuery { newQuery }

    /**
     * Atomically transforms the current query using the given function.
     */
    fun editQuery(transform: (State<T, S>) -> PageQuery<S>) {
        synchronized(this.lock) {
            val currentState = this.state
            val newQuery = transform(currentState)

            if (newQuery != currentState.query) {
                this.state = State(
                    query = newQuery,
                    result = currentState.result,
                    isStale = true,
                )
            }
        }
    }

    /**
     * Fetches data for the given query.
     *
     * @return true if the fetch succeeded or the query was changed, false if the fetch failed.
     */
    suspend fun fetch(newQuery: PageQuery<S>) = fetch { newQuery }

    /**
     * Fetches data by transforming the current query.
     * If no transform is provided, fetches the current query.
     *
     * @return true if the fetch succeeded or the query was changed, false if the fetch failed.
     */
    suspend fun fetch(transform: (State<T, S>) -> PageQuery<S> = { it.query }): Boolean {
        val fetchQuery = synchronized(this.lock) {
            val currentState = this.state
            val newQuery = transform(currentState)

            if (newQuery != currentState.query) {
                this.state = State(
                    query = newQuery,
                    result = currentState.result,
                    isStale = true,
                )
            }

            newQuery
        }

        val fetchResult = try {
            synchronized(this.lock) {
                this._isLoading++
            }

            fetcher(fetchQuery) ?: return fetchQuery != state.query
        } catch (e: Throwable) {
            e.throwIfFatal()
            errors += e
            return fetchQuery != state.query
        } finally {
            synchronized(this.lock) {
                this._isLoading--
            }
        }

        synchronized(this.lock) {
            val currentState = this.state

            if (fetchQuery == currentState.query) {
                this.state = State(
                    query = fetchQuery,
                    result = fetchResult,
                    isStale = false,
                )
            }
        }

        return true
    }
}
