package mega.privacy.android.domain.entity.chat.messages.meta

/**
 * Chat rich preview info
 */
interface ChatRichPreviewInfo {
    /**
     * Title
     */
    val title: String

    /**
     * Description
     */
    val description: String

    /**
     * Image
     */
    val image: String?

    /**
     * Image format
     */
    val imageFormat: String?

    /**
     * Icon
     */
    val icon: String?

    /**
     * Icon format
     */
    val iconFormat: String?

    /**
     * Url
     */
    val url: String

    /**
     * Domain name
     */
    val domainName: String
}