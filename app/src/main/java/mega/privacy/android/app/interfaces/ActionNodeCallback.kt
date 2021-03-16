package mega.privacy.android.app.interfaces

interface ActionNodeCallback {

    /**
     * Completes the rename action with success.
     */
    fun finishRenameActionWithSuccess()

    /**
     * Makes the necessary UI changes after confirm the action.
     */
    fun actionConfirmed()

    /**
     * Confirms the action to create a new folder.
     *
     * @param folderName Name of the new folder.
     */
    fun createFolder(folderName: String)
}