package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.queue.QueueEvent

interface NavigationEventQueueReceiver {
    val events: ReceiveChannel<() -> QueueEvent?>
}
