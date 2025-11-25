package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.ReceiveChannel

interface NavigationEventQueueReceiver {
    val events: ReceiveChannel<() -> List<NavKey>?>
}
