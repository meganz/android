package mega.privacy.android.domain.entity

/**
 * Event class that from MegaSDK
 *
 * @property handle
 * @property eventString
 * @property number
 * @property text
 * @property type
 */
data class Event(
    val handle: Long,
    val eventString: String,
    val number: Long,
    val text: String,
    val type: EventType,
)