package mega.privacy.android.navigation.contract.queue.dialog

import mega.privacy.android.navigation.contract.queue.NavPriority

interface AppDialogsEventQueue {
    suspend fun emit(event: AppDialogEvent, priority: NavPriority = NavPriority.Default)
}