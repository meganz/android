package mega.privacy.android.app.presentation.bottomsheet

/**
 * Show new text file dialog action listener
 */
fun interface ShowNewTextFileDialogActionListener {
    /**
     * Show new text file dialog
     *
     * @param typedName
     */
    fun showNewTextFileDialog(typedName: String?)
}