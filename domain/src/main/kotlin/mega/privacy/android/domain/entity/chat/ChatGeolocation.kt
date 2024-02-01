package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo

/**
 * Data class for storing geolocation data.
 *
 * @property longitude ChatGeolocation longitude.
 * @property latitude ChatGeolocation latitude.
 * @property image ChatGeolocation preview as a byte array encoded in Base64URL or null if not available.
 */
data class ChatGeolocation(
    override val longitude: Float,
    override val latitude: Float,
    override val image: String?,
) : ChatGeolocationInfo