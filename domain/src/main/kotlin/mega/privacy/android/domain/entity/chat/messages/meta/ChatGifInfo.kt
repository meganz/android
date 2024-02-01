package mega.privacy.android.domain.entity.chat.messages.meta

/**
 * Chat gif info
 */
interface ChatGifInfo {
    /**
     * Mp4src
     */
    val mp4Src: String?

    /**
     * Webp src
     */
    val webpSrc: String?

    /**
     * Title
     */
    val title: String?

    /**
     * Mp4size
     */
    val mp4Size: Int

    /**
     * Webp size
     */
    val webpSize: Int

    /**
     * Width
     */
    val width: Int

    /**
     * Height
     */
    val height: Int
}