package mega.privacy.android.navigation.contract.queue

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavOptions

interface NavigationEventQueue {
    suspend fun emit(
        navKey: NavKey,
        priority: NavPriority = NavPriority.Default,
        navOptions: NavOptions? = null,
    )

    suspend fun emit(
        navKeys: List<NavKey>,
        priority: NavPriority = NavPriority.Default,
        navOptions: NavOptions? = null,
    )
}