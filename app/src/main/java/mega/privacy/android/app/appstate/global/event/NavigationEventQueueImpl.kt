package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationEventQueueImpl(
    private val getTime: () -> Long,
) : NavigationEventQueue,
    NavigationEventQueueReceiver {

    @Inject
    constructor() : this(getTime = System::currentTimeMillis)

    private val queueChannel = QueuePollingChannel(
        queue = PriorityQueue<QueuedEvent>(
            compareByDescending<QueuedEvent> { it.priority }
                .thenBy { it.timestamp }
        ),
        mapper = { it?.event }
    )

    override val events: ReceiveChannel<() -> QueueEvent?>
        get() = queueChannel.events

    private data class QueuedEvent(
        val event: QueueEvent,
        val priority: NavPriority,
        val timestamp: Long,
    )

    override suspend fun emit(navKeys: List<NavKey>, priority: NavPriority) {
        queueChannel.add(QueuedEvent(NavigationQueueEvent(navKeys), priority, getTime()))
    }

    override suspend fun emit(navKey: NavKey, priority: NavPriority) =
        emit(listOf(navKey), priority)

}
