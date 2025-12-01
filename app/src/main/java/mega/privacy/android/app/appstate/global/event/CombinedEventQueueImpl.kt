package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedEventQueueImpl(
    private val getTime: () -> Long,
    private val queueEventComparator: QueueEventComparator,
) : NavigationEventQueue,
    AppDialogsEventQueue,
    NavigationEventQueueReceiver {

    @Inject
    constructor(
        queueEventComparator: QueueEventComparator,
    ) : this(getTime = System::currentTimeMillis, queueEventComparator = queueEventComparator)

    private val queue = PriorityQueue(
        compareByDescending<QueuedEvent, QueueEvent>(queueEventComparator) { it.event }
            .thenByDescending { it.priority }
            .thenBy { it.timestamp }
    )

    private val queueChannel = QueuePollingChannel(
        queue = queue,
        mapper = { it?.event }
    )

    override val events: ReceiveChannel<() -> QueueEvent?>
        get() = queueChannel.events


    override suspend fun emit(navKeys: List<NavKey>, priority: NavPriority) {
        queueChannel.add(
            QueuedEvent(
                event = NavigationQueueEvent(keys = navKeys),
                priority = priority,
                timestamp = getTime()
            )
        )
    }

    override suspend fun emit(navKey: NavKey, priority: NavPriority) =
        emit(listOf(navKey), priority)

    override suspend fun emit(
        event: AppDialogEvent,
        priority: NavPriority,
    ) {
        queueChannel.add(
            QueuedEvent(event = event, priority = priority, timestamp = getTime())
        )
    }

}