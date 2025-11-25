package mega.privacy.android.app.appstate.global.event

import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.QueueEvent

internal data class QueuedEvent(
    val event: QueueEvent,
    val priority: NavPriority,
    val timestamp: Long,
)