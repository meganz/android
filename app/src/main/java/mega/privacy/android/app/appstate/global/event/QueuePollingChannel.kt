package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Queue

class QueuePollingChannel<T, R>(
    private val queue: Queue<R>,
    private val mapper: (R?) -> T?,
) {
    private val mutex = Mutex()
    val events: Channel<() -> T?> = Channel<() -> T?>(Channel.UNLIMITED)

    suspend fun add(item: R) {
        var sendEvent = false

        mutex.withLock {
            sendEvent = queue.isEmpty()
            queue.add(item)
        }

        if (sendEvent) {
            events.send { pollAndCheckEvents() }
        }
    }

    private fun pollAndCheckEvents(): T? = mapper(queue.poll())?.also {
        if (queue.isNotEmpty()) {
            events.trySend { pollAndCheckEvents() }
        }
    }
}