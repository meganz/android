package mega.privacy.android.navigation.contract.queue.dialog

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.queue.QueueEvent

class AppDialogEvent(
    val dialogDestination: NavKey,
) : QueueEvent
