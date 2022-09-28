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
abstract class Event(
    open val handle: Long,
    open val eventString: String,
    open val number: Long,
    open val text: String,
    open val type: EventType,
)

/**
 * Sub type of [Event] for Storage Event
 *
 * @property storageState storage state
 */
data class StorageStateEvent(
    override val handle: Long,
    override val eventString: String,
    override val number: Long,
    override val text: String,
    override val type: EventType,
    val storageState: StorageState,
) : Event(handle, eventString, number, text, type)

/**
 * Sub type of [Event] for normal types
 */
data class NormalEvent(
    override val handle: Long,
    override val eventString: String,
    override val number: Long,
    override val text: String,
    override val type: EventType,
) : Event(handle, eventString, number, text, type)
