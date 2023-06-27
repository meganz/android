package mega.privacy.android.domain.entity.chat

/**
 * Data class for storing geolocation data.
 *
 * @property longitude ChatGeolocation longitude.
 * @property latitude ChatGeolocation latitude.
 * @property image ChatGeolocation preview as a byte array encoded in Base64URL or null if not available.
 */
data class ChatGeolocation(
    val longitude: Float,
    val latitude: Float,
    val image: String?,
)