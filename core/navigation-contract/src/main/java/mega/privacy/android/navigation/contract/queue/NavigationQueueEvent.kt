package mega.privacy.android.navigation.contract.queue

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.navkey.Suppressable

/**
 * Navigation queue event
 *
 * @property keys
 * @property navOptions
 */
class NavigationQueueEvent(
    val keys: List<NavKey>,
    val navOptions: NavOptions? = null,
) : QueueEvent {
    override val suppressableKey: NavKey?
        get() = keys.last().takeIf { it is Suppressable }
}