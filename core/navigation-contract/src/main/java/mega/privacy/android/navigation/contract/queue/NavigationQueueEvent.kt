package mega.privacy.android.navigation.contract.queue

import androidx.navigation3.runtime.NavKey

data class NavigationQueueEvent(val keys: List<NavKey>) : QueueEvent