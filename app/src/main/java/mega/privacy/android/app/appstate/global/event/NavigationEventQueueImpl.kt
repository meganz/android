package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
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
        mapper = { it?.navKeys }
    )

    override val events: ReceiveChannel<() -> List<NavKey>?>
        get() = queueChannel.events

    private data class QueuedEvent(
        val navKeys: List<NavKey>,
        val priority: NavPriority,
        val timestamp: Long,
    )

    override suspend fun emit(navKeys: List<NavKey>, priority: NavPriority) {
        queueChannel.add(QueuedEvent(navKeys, priority, getTime()))
    }

    override suspend fun emit(navKey: NavKey, priority: NavPriority) =
        emit(listOf(navKey), priority)


}