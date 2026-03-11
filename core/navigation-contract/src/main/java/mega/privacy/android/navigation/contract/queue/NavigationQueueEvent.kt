package mega.privacy.android.navigation.contract.queue

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavOptions

class NavigationQueueEvent(
    val keys: List<NavKey>,
    val navOptions: NavOptions? = null,
) : QueueEvent