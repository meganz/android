package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.dialog.AppDialogsEventQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDialogsEventQueueImpl @Inject constructor() : AppDialogsEventQueue,
    AppDialogsEventQueueReceiver {
    private val _events = Channel<AppDialogEvent>(capacity = 10)
    override val events: ReceiveChannel<AppDialogEvent> = _events

    override suspend fun emit(event: AppDialogEvent) {
        _events.send(event)
    }
}