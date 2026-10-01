package mega.privacy.android.domain.repository

/**
 * Repository class to access system clip board.
 */
interface ClipboardRepository {

    /**
     * Set a text in system clipboard, with a label.
     *
     * @param label user visible label for the clip data
     * @param text to be set in the clipboard
     */
    fun setClip(label: String, text: String)
}