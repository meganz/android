package mega.privacy.android.navigation.contract.dialog

interface AppDialogsEventQueue {
    suspend fun emit(event: AppDialogEvent)
}