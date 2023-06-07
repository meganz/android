package mega.privacy.android.domain.entity.analytics.identifier

/**
 * Menu item event identifier
 *
 * @property menuItem
 * @property uniqueIdentifier
 * @property screenName
 * @property menuType
 * @constructor Create empty Menu item event identifier
 */
data class MenuItemEventIdentifier(
    val menuItem: String,
    val uniqueIdentifier: Int,
    val screenName: String?,
    val menuType: String?,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}