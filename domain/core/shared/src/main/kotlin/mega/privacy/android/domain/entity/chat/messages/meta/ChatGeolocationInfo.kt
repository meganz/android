package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Polymorphic

/**
 * Chat geolocation info
 */
@Polymorphic
interface ChatGeolocationInfo {
    /**
     * Longitude
     */
    val longitude: Float

    /**
     * Latitude
     */
    val latitude: Float

    /**
     * Image
     */
    val image: String?
}