package mega.privacy.android.data.extensions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

/**
 * Returns a flow that invokes the given [action] before the first value of the upstream matching the given [predicate] is emitted
 */
fun <T> Flow<T>.onFirst(
    predicate: (T) -> Boolean,
    action: suspend (T) -> Unit,
): Flow<T> {
    var consumed = false
    return this.onEach { value ->
        if (!consumed && predicate(value)) {
            consumed = true
            action(value)
        }
    }
}

/**
 * Returns a new flow where values not validated by [validator] will be skip if a new value is emitted before the delay
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.skipUnstable(timeout: Duration, validator: (T) -> Boolean): Flow<T> {
    return this.mapLatest { value ->
        if (validator(value).not()) {
            delay(timeout)
        }
        value
    }
}

/**
 * Similar to `onEach`, but the [action] will only be executed if either:
 * - The specified [period] has passed since the last execution.
 * - The [shouldEmitImmediately] function returns `true` for the new value.
 *
 * Unlike `Flow.sample`, if a value is skipped, it will not be executed later. The [action] is only triggered
 * at the moment a new value is emitted and meets the criteria.
 *
 * The [action] will always run for the first emitted value but may be skipped for the last one.
 *
 * @param period The minimum duration between executions of [action], unless [shouldEmitImmediately] forces it.
 * @param shouldEmitImmediately A function that determines whether a new value should trigger the [action] immediately, regardless of [period].
 * @param getCurrentTimeMillis optional parameter to inject current time in millis for testing purposes
 * @param action The action to perform on each sampled emission.
 */
fun <T> Flow<T>.onEachSampled(
    period: Duration,
    shouldEmitImmediately: ((previous: T?, new: T) -> Boolean)? = null,
    getCurrentTimeMillis: () -> Long = { System.currentTimeMillis() },
    action: suspend (T) -> Unit,
): Flow<T> {
    var lastEmission: Long =
        Long.MIN_VALUE + 1 // +1 to avoid overflow on tests where current time can be 0
    var previous: T? = null
    return this.transform {
        val now = getCurrentTimeMillis()
        if (period == Duration.ZERO
            || shouldEmitImmediately?.invoke(previous, it) == true
            || (now - lastEmission).milliseconds >= period
        ) {
            lastEmission = now
            action(it)
        }
        previous = it
        return@transform emit(it)
    }
}