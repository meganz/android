package mega.privacy.android.app.appstate.global.event

import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.QueueEvent

internal data class QueuedEvent(
    val event: QueueEvent,
    val priority: NavPriority,
    val timestamp: Long,
){
    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            !is QueuedEvent -> false
            else -> event == other.event
        }
    }

    override fun hashCode(): Int {
        return event.hashCode()
    }
}