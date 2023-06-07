package mega.privacy.android.domain.entity.analytics.identifier

/**
 * Navigation event identifier
 *
 * @property uniqueIdentifier
 * @property navigationElementType
 * @property destination
 * @constructor Create empty Navigation event identifier
 */
data class NavigationEventIdentifier(
    val uniqueIdentifier: Int,
    val navigationElementType: String?,
    val destination: String?,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}