package mega.privacy.android.app.interfaces

interface UpdateNodeCallback {

    /**
     * Completes the rename action with success.
     */
    fun finishRenameActionWithSuccess()

    /**
     * Makes the necessary UI changes after confirm the action.
     */
    fun actionConfirmed()
}