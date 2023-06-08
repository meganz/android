package mega.privacy.android.domain.entity.analytics.identifier

/**
 * Notification event identifier
 *
 * @property name
 * @property uniqueIdentifier
 * @constructor Create empty Notification event identifier
 */
data class NotificationEventIdentifier(
    val name: String,
    val uniqueIdentifier: Int,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}