package mega.privacy.android.app.appstate.global.event

import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.dialog.AppDialogEvent

interface AppDialogsEventQueueReceiver {
    val events: ReceiveChannel<AppDialogEvent>
}
