package mega.privacy.android.domain.extension

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Extension function for asynchronously transforming each element of an [Iterable] using a given [transformation] function.
 *
 * This function provides flexible concurrency strategies for different scenarios:
 * - [ConcurrencyStrategy.Sequential]: No concurrency, sequential processing
 * - [ConcurrencyStrategy.ParallelWithLimit]: Fixed concurrency limit with semaphore
 * - [ConcurrencyStrategy.ChunkedParallel]: Process chunks in parallel, then join sequentially
 * - [ConcurrencyStrategy.ChunkedSequential]: Process chunks sequentially, but elements within each chunk in parallel
 * - [ConcurrencyStrategy.ChunkedWithLimit]: Process in chunks with controlled concurrency per chunk
 * - [ConcurrencyStrategy.DynamicChunked]: Process in chunks, calculated based on item count
 *
 * @param strategy The concurrency strategy to use. Defaults to [ConcurrencyStrategy.ParallelWithLimit] with value 10.
 * @param transformation A suspend function that transforms an element of type [T] into a result of type [R].
 * @return A list of results of type [R] after applying the [transformation] to each element of the iterable.
 * @throws Exception Any exception thrown during transformation will propagate.
 *
 * Example usage:
 * ```
 * // Sequential processing (no concurrency)
 * val results = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.Sequential) { processElement(it) }
 *
 * // Unlimited parallel processing
 * val parallelResults = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.Parallel) { processElement(it) }
 *
 * // Fixed concurrency limit
 * val limitedResults = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.ParallelWithLimit(5)) { processElement(it) }
 *
 * // Chunked processing with parallel chunks
 * val parallelChunked = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.ChunkedParallel(100)) { processElement(it) }
 *
 * // Chunked processing with sequential chunks but parallel elements
 * val sequentialChunked = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.ChunkedSequential(100)) { processElement(it) }
 *
 * // Chunked processing with controlled concurrency
 * val controlledChunked = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.ChunkedWithLimit(25, 500)) { processElement(it) }
 *
 * // Dynamic chunked processing where chunk is calculated by items count
 * val dynamicChunked = listOf("a", "b", "c").mapAsync(ConcurrencyStrategy.DynamicChunked(25))
 * ```
 */
suspend fun <T, R> Iterable<T>.mapAsync(
    strategy: ConcurrencyStrategy = ConcurrencyStrategy.ParallelWithLimit(10),
    transformation: suspend (T) -> R,
): List<R> {
    val count = this.count()

    return when {
        count == 0 -> emptyList()
        count == 1 -> map { transformation(it) }
        strategy is ConcurrencyStrategy.Sequential -> {
            // Sequential processing
            map { transformation(it) }
        }

        strategy is ConcurrencyStrategy.Parallel -> {
            coroutineScope {
                map { async { transformation(it) } }.awaitAll()
            }
        }

        strategy is ConcurrencyStrategy.ParallelWithLimit -> {
            // Fixed concurrency limit
            val semaphore = Semaphore(strategy.value)
            coroutineScope {
                map {
                    async { semaphore.withPermit { transformation(it) } }
                }.awaitAll()
            }
        }

        strategy is ConcurrencyStrategy.ChunkedParallel -> {
            // Process chunks in parallel, then join sequentially
            coroutineScope {
                chunked(strategy.chunkSize).map { chunk ->
                    async {
                        chunk.map { element ->
                            transformation(element)
                        }
                    }
                }.awaitAll()
            }.flatten()
        }

        strategy is ConcurrencyStrategy.ChunkedSequential -> {
            // Process chunks sequentially, but elements within each chunk in parallel
            val results = mutableListOf<R>()
            chunked(strategy.chunkSize).forEach { chunk ->
                val chunkResults = coroutineScope {
                    chunk.map { element ->
                        async { transformation(element) }
                    }.awaitAll()
                }
                results.addAll(chunkResults)
            }
            results
        }

        strategy is ConcurrencyStrategy.ChunkedWithLimit -> {
            // Chunked processing with controlled concurrency
            val semaphore = Semaphore(strategy.limit)
            coroutineScope {
                chunked(strategy.chunkSize).map { chunk ->
                    async {
                        semaphore.withPermit {
                            chunk.map { element ->
                                transformation(element)
                            }
                        }
                    }
                }.awaitAll()
            }.flatten()
        }

        strategy is ConcurrencyStrategy.DynamicChunked -> {
            // Dynamic chunk size based on collection size
            val dynamicChunkSize = if (count > 1000) {
                count / 20 // ~20 chunks
            } else {
                count
            }
            val semaphore = Semaphore(strategy.limit)
            coroutineScope {
                chunked(dynamicChunkSize).map { chunk ->
                    async {
                        semaphore.withPermit {
                            chunk.map { element ->
                                transformation(element)
                            }
                        }
                    }
                }.awaitAll().flatten()
            }
        }

        // Fallback to sequential processing
        else -> map { transformation(it) }
    }
}

/**
 * Defines different strategies for handling concurrency in collection processing operations.
 *
 * Each strategy provides a different approach to balancing performance, memory usage, and resource control.
 */
sealed class ConcurrencyStrategy {
    /**
     * No concurrency - processes elements sequentially.
     * Best for: Small collections and when predictable order is required.
     */
    object Sequential : ConcurrencyStrategy()

    /**
     * Unlimited concurrency - processes all elements in parallel.
     * Best for: Smaller list with controlled resource usage, memory-constrained environments.
     */
    object Parallel : ConcurrencyStrategy()

    /**
     * Fixed concurrency limit using a semaphore.
     * Best for: Controlled resource usage, API rate limiting, memory-constrained environments.
     *
     * @param value Maximum number of concurrent operations allowed
     */
    data class ParallelWithLimit(val value: Int) : ConcurrencyStrategy()

    /**
     * Process chunks in parallel, then join results sequentially.
     * Best for: Large collections where you want maximum parallelism across chunks.
     *
     * Execution pattern: All chunks start processing simultaneously, results are collected when all complete.
     *
     * @param chunkSize Size of each chunk to process
     */
    data class ChunkedParallel(val chunkSize: Int) : ConcurrencyStrategy()

    /**
     * Process chunks sequentially, but elements within each chunk in parallel.
     * Best for: Large collections where you want controlled memory usage and predictable chunk ordering.
     *
     * Execution pattern: Process chunk 1 completely, then chunk 2, then chunk 3, etc.
     *
     * @param chunkSize Size of each chunk to process
     */
    data class ChunkedSequential(val chunkSize: Int) : ConcurrencyStrategy()

    /**
     * Process chunks with controlled concurrency per chunk.
     * Best for: Large collections where you need both chunking and resource control.
     *
     * @param limit Maximum concurrent operations per chunk
     * @param chunkSize Size of each chunk to process
     */
    data class ChunkedWithLimit(val limit: Int, val chunkSize: Int) : ConcurrencyStrategy()

    /**
     * Dynamically adjust chunk size based on collection size.
     * Best for: Very large collections where optimal chunk size is unknown.
     *
     * @param limit Maximum concurrent operations per chunk
     */
    data class DynamicChunked(val limit: Int) : ConcurrencyStrategy()
}


