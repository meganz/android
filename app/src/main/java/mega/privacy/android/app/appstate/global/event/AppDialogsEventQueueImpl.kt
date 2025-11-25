package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDialogsEventQueueImpl(
    private val getTime: () -> Long,
) : AppDialogsEventQueue,
    AppDialogsEventQueueReceiver {

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

    override suspend fun emit(event: AppDialogEvent, priority: NavPriority) {
        queueChannel.add(
            QueuedEvent(event = event, priority = priority, timestamp = getTime())
        )
    }
}