package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.Queue

class QueuePollingChannel<T, R>(
    private val queue: Queue<R>,
    private val mapper: (R?) -> T?,
) {
    private val mutex = Mutex()
    val events: Channel<suspend () -> T?> = Channel<suspend () -> T?>(Channel.UNLIMITED)

    suspend fun add(item: R) {
        var sendEvent = false

        mutex.withLock {
            sendEvent = queue.isEmpty()
            if (queue.contains(item).not()) {
                queue.add(item)
            } else {
                Timber.d("Item already in queue: $item")
            }
        }

        if (sendEvent) {
            events.send { pollAndCheckEvents() }
        }
    }

    private suspend fun pollAndCheckEvents(): T? {
        val polled: R?
        val hasMore: Boolean
        mutex.withLock {
            polled = queue.poll()
            hasMore = queue.isNotEmpty()
        }
        return mapper(polled)?.also {
            if (hasMore) {
                events.trySend { pollAndCheckEvents() }
            }
        }
    }
}