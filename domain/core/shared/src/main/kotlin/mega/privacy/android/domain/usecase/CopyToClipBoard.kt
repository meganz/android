package mega.privacy.android.domain.usecase

/**
 * Use case to copy text into system clipboard
 */
fun interface CopyToClipBoard {
    /**
     * invoke method
     *
     * @param label label of the clip
     * @param text value of the clip
     */
    operator fun invoke(label: String, text: String)
}