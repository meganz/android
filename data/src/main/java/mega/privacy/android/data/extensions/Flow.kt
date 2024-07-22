package mega.privacy.android.data.extensions

import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

private class TimeChunkedFlow<T>(
    private val upstream: Flow<T>,
    private val chunkDuration: Duration,
    private val flushOnIdleDuration: Duration,
) : Flow<List<T>> {
    private var _pendingValues: List<T> = emptyList()
    private val mutex = Mutex()
    private val values = mutableListOf<T>()
    private var flushScheduled = false
    private var noEventsFlushJob: Job? = null

    val pendingValues: List<T>
        get() = _pendingValues

    override suspend fun collect(collector: FlowCollector<List<T>>) = coroutineScope {
        upstream.collect {
            mutex.withLock {
                values.add(it)
            }
            if (flushOnIdleDuration < chunkDuration) {
                noEventsFlushJob?.cancel()
                noEventsFlushJob = launch {
                    delay(flushOnIdleDuration)
                    flushEvents(collector)
                }
            }
            if (!flushScheduled) {
                flushScheduled = true
                launch {
                    try {
                        delay(chunkDuration)
                        noEventsFlushJob?.cancel()
                        flushEvents(collector)
                    } catch (e: CancellationException) {
                        _pendingValues = values
                        throw e
                    }
                }
            }
        }
    }

    private suspend fun flushEvents(collector: FlowCollector<List<T>>) {
        flushScheduled = false
        mutex.withLock {
            withContext(NonCancellable) {
                val toEmit = values.toList()
                values.clear()
                _pendingValues = emptyList()
                if (toEmit.isNotEmpty()) {
                    collector.emit(toEmit.toList())
                }
            }
        }
    }
}

/**
 * Exception indicating that the flow has been cancelled with some received but not emitted values.
 * @property pendingValues list of not emitted values when the flow was cancelled
 */
class ChunkCancelledWithPendingValuesException(
    val pendingValues: List<*>,
    original: CancellationException,
) : CancellationException("Flow cancelled with not emitted pending values") {

    init {
        this.initCause(original)
    }
}

/**
 * Returns a flow emitting every [chunkDuration] a list of the values received since last emission, if any.
 *
 * If [flushOnIdleDuration] is lower than [chunkDuration] and no new values are received during [flushOnIdleDuration] a new list will be emitted
 *
 * If the flow is cancelled and there are pending values to emit, a [ChunkCancelledWithPendingValuesException] with the pending values is thrown to cancel the flow instead of original cancellation exception.
 * This can be checked in on completion, example:
 * ```
 * .onCompletion {
 *     if (it is ChunkCancelledWithPendingValuesException) {
 *         pendingValues = it.pendingValues.filterIsInstance<T>()
 *         //do whatever to process pending values
 *     }
 * }
 *```
 *
 * @param chunkDuration duration of the chunks to emit
 * @param flushOnIdleDuration duration to emit the chunk if no values are received in this period
 * @return flow emitting every [chunkDuration] a list of the values received since last
 *
 */
fun <T> Flow<T>.chunked(
    chunkDuration: Duration,
    flushOnIdleDuration: Duration = chunkDuration,
): Flow<List<T>> = TimeChunkedFlow(this, chunkDuration, flushOnIdleDuration)
    .let {
        it.onCompletion { e ->
            if (e is CancellationException && it.pendingValues.isNotEmpty()) {
                throw ChunkCancelledWithPendingValuesException(it.pendingValues, e)
            } else if (e != null) {
                throw e
            }
        }
    }

/**
 * Returns a flow emitting every [chunkDuration] a list of the values received since last emission, if any.
 *
 * If [flushOnIdleDuration] is lower than [chunkDuration] and no new values are received during [flushOnIdleDuration] a new list will be emitted
 *
 * If the flow is cancelled and there are pending values to emit, they are emitted in the collector before the flow is cancelled.
 *
 * @param chunkDuration duration of the chunks to emit
 * @param flushOnIdleDuration duration to emit the chunk if no values are received in this period
 * @param collector collector to emit the chunks
 * @return flow emitting every [chunkDuration] a list of the values received since last
 */
suspend fun <T> Flow<T>.collectChunked(
    chunkDuration: Duration,
    flushOnIdleDuration: Duration = chunkDuration,
    collector: FlowCollector<List<T>>,
) = TimeChunkedFlow(this, chunkDuration, flushOnIdleDuration)
    .let {
        it.onCompletion { e ->
            if (e is CancellationException && it.pendingValues.isNotEmpty()) {
                collector.emit(it.pendingValues)
            } else if (e != null) {
                throw e
            }
        }
    }.collect(collector)