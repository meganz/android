package mega.privacy.android.domain.extension

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Extension function for asynchronously transforming each element of an [Iterable] using a given [transformation] function.
 *
 * If the size of the iterable is 1, the transformation is applied directly without concurrency, avoiding launching unnecessary coroutine and creating scope, semaphore, etc.
 * Otherwise, the transformation is applied asynchronously with a limit on concurrency controlled by [concurrencyLimit].
 *
 * The function uses a semaphore to control the number of concurrent transformations being executed at any given time.
 *
 * @param concurrencyLimit The maximum number of concurrent transformations allowed. Defaults to 10.
 * @param transformation A suspend function that transforms an element of type [T] into a result of type [R].
 * @return A list of results of type [R] after applying the [transformation] to each element of the iterable.
 * @throws Exception Any exception thrown during transformation will propagate.
 *
 * Example usage:
 * ```
 * val results = listOf("a", "b", "c").mapAsync { processElement(it) }
 * ```
 */
suspend fun <T, R> Iterable<T>.mapAsync(
    concurrencyLimit: Int = 10,
    transformation: suspend (T) -> R,
): List<R> = if (this.count() > 1)
    coroutineScope {
        val semaphore = Semaphore(concurrencyLimit)
        this@mapAsync
            .map { async { semaphore.withPermit { transformation(it) } } }
            .awaitAll()
    } else map { transformation(it) }

