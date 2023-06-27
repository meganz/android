package mega.privacy.android.domain.entity.chat

/**
 * Data class for storing rich preview data.
 *
 * @property title Rich preview title.
 * @property description Rich preview description.
 * @property image Rich preview image as byte array encoded in Base64URL or null if not available.
 * @property imageFormat Rich preview image format.
 * @property icon Rich preview icon as byte array encoded in Base64URL or null if not available.
 * @property iconFormat Rich preview icon format.
 * @property url Rich preview url.
 * @property domainName Domain name from rich preview url.
 */
data class RichPreview(
    val title: String,
    val description: String,
    val image: String?,
    val imageFormat: String?,
    val icon: String?,
    val iconFormat: String?,
    val url: String,
    val domainName: String,
)
