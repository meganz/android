package mega.privacy.android.app.presentation.bottomsheet

/**
 * Show new folder dialog action listener
 */
fun interface ShowNewFolderDialogActionListener {
    /**
     * Show new folder dialog
     *
     * @param typedText
     */
    fun showNewFolderDialog(typedText: String?)
}