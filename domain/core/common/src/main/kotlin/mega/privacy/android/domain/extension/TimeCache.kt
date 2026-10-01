package mega.privacy.android.domain.extension

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.Duration

/**
 * A generic time-based cache that asynchronously loads values and caches them for a specified duration.
 *
 * @param T The type of the value being cached.
 * @property timeToLive Time-to-live in Duration. After this duration, the cached value is considered stale.
 * @property timeProvider A function that returns the current time in milliseconds, used for testing and flexibility.
 * @property loader A suspend function that provides a fresh value when the cache is empty or stale.
 */
class TimeCache<T>(
    private val timeToLive: Duration,
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
    private val loader: suspend () -> T,
) {
    @Volatile
    private var cachedValue: T? = null

    @Volatile
    private var expiresAt: Long = 0L

    private val mutex = Mutex()

    /**
     * Retrieves the value from the cache.
     * If a fresh value exists, it is returned immediately.
     * Otherwise, a new value is loaded, cached, and returned.
     * This method is thread-safe and ensures only one loading operation happens at a time.
     *
     * @return The cached or freshly loaded value.
     */
    suspend fun get(): T {
        // FAST PATH
        val cached = cachedValue
        if (cached != null && timeProvider() < expiresAt) {
            return cached
        }

        // SLOW PATH
        return mutex.withLock {
            // Double-check
            val cachedInLock = cachedValue
            if (cachedInLock != null && timeProvider() < expiresAt) {
                return cachedInLock
            }

            val value = loader()
            cachedValue = value
            expiresAt = timeProvider() + timeToLive.inWholeMilliseconds
            value
        }
    }

    /**
     * Forces a refresh of the cached value by ignoring any existing entry and invoking the loader.
     *
     * @return The freshly loaded value.
     */
    suspend fun refresh(): T {
        return mutex.withLock {
            val value = loader()
            cachedValue = value
            expiresAt = timeProvider() + timeToLive.inWholeMilliseconds
            value
        }
    }

    /**
     * Invalidates the current cache entry, causing the next [get] call to load a fresh value.
     */
    fun invalidate() {
        expiresAt = 0L
    }

    /**
     * Checks if the cache currently contains a fresh (non-expired) value.
     *
     * @return True if the cache is fresh, false otherwise.
     */
    fun isFresh(): Boolean {
        return cachedValue != null && timeProvider() < expiresAt
    }

    /**
     * Returns the age of the current cache entry in milliseconds.
     *
     * @return The age in milliseconds, or null if there is no cached value.
     */
    fun age(): Long? {
        val expires = expiresAt
        return if (expires > 0) {
            (timeProvider() - (expires - timeToLive.inWholeMilliseconds)).coerceAtLeast(0)
        } else null
    }
}
