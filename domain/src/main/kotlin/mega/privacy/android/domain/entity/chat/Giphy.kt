package mega.privacy.android.domain.entity.chat

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
    val mp4Src: String?,
    val webpSrc: String?,
    val title: String?,
    val mp4Size: Int,
    val webpSize: Int,
    val width: Int,
    val height: Int,
)
