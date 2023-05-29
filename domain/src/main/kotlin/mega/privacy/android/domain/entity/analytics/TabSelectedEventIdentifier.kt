package mega.privacy.android.domain.entity.analytics

/**
 * Tab selected event identifier
 *
 * @property screenName
 * @property tabName
 * @property uniqueIdentifier
 * @constructor Create empty Tab selected event identifier
 */
data class TabSelectedEventIdentifier(
    val screenName: String,
    val tabName: String,
    val uniqueIdentifier: Int,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}