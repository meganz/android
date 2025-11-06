package mega.privacy.android.navigation.contract.queue

import androidx.navigation3.runtime.NavKey

interface NavigationEventQueue {
    suspend fun emit(navKey: NavKey)
}