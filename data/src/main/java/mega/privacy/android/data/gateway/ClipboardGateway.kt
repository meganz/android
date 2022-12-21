package mega.privacy.android.data.gateway

/**
 * Gateway to access the system clipboard
 */
interface ClipboardGateway {
    /**
     * Set a text in system clipboard, with a label.
     *
     * @param label user visible label for the clip data
     * @param text to be set in the clipboard
     */
    fun setClip(label: String, text: String)
}