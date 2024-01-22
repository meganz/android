package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.chat.messages.ChatGifInfo

/**
 * Data class for storing giphy data.
 *
 * @property mp4Src Source of the mp4.
 * @property webpSrc Source of the webp.
 * @property title Title of the giphy.
 * @property mp4Size Size of the mp4.
 * @property webpSize Size of the webp.
 * @property width Width of the giphy.
 * @property height Height of the giphy.
 */
data class Giphy(
    override val mp4Src: String?,
    override val webpSrc: String?,
    override val title: String?,
    override val mp4Size: Int,
    override val webpSize: Int,
    override val width: Int,
    override val height: Int,
) : ChatGifInfo
