package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.chat.messages.ChatRichPreviewInfo

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
    override val title: String,
    override val description: String,
    override val image: String?,
    override val imageFormat: String?,
    override val icon: String?,
    override val iconFormat: String?,
    override val url: String,
    override val domainName: String,
) : ChatRichPreviewInfo
