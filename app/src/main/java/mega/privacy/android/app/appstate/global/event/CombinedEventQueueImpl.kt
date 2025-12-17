package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
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


    override suspend fun emit(navKeys: List<NavKey>, priority: NavPriority, isSingleTop: Boolean) {
        Timber.d("Emit navigation events: $navKeys")

        val pendingNavKeys = mutableListOf<NavKey>()
        for (navKey in navKeys) {
            if (navKey is DialogNavKey) {
                if (pendingNavKeys.isNotEmpty()) {
                    queueChannel.add(
                        QueuedEvent(
                            event = NavigationQueueEvent(
                                keys = pendingNavKeys.toList(),
                                isSingleTop = isSingleTop
                            ),
                            priority = priority,
                            timestamp = getTime()
                        )
                    )
                    pendingNavKeys.clear()
                }

                queueChannel.add(
                    QueuedEvent(
                        event = AppDialogEvent(navKey),
                        priority = priority,
                        timestamp = getTime()
                    )
                )
            } else {
                pendingNavKeys += navKey
            }
        }

        if (pendingNavKeys.isNotEmpty()) {
            queueChannel.add(
                QueuedEvent(
                    event = NavigationQueueEvent(keys = pendingNavKeys, isSingleTop = isSingleTop),
                    priority = priority,
                    timestamp = getTime()
                )
            )
        }
    }

    override suspend fun emit(navKey: NavKey, priority: NavPriority, isSingleTop: Boolean) =
        emit(listOf(navKey), priority, isSingleTop)

    override suspend fun emit(
        event: AppDialogEvent,
        priority: NavPriority,
    ) {
        Timber.d("Emit dialog event: $event")
        queueChannel.add(
            QueuedEvent(event = event, priority = priority, timestamp = getTime())
        )
    }

}