package mega.privacy.android.navigation.contract.queue.dialog

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.navkey.Suppressable
import mega.privacy.android.navigation.contract.queue.QueueEvent

/**
 * App dialog event
 *
 * @property dialogDestination
 */
class AppDialogEvent(
    val dialogDestination: NavKey,
) : QueueEvent {
    override val isSuppressable: Boolean
        get() = dialogDestination is Suppressable
}
