package mega.privacy.android.domain.entity.analytics.identifier

/**
 * Dialog displayed event identifier
 *
 * @property screenName
 * @property dialogName
 * @property uniqueIdentifier
 * @constructor Create empty Dialog displayed event identifier
 */
data class DialogDisplayedEventIdentifier(
    val screenName: String?,
    val dialogName: String,
    val uniqueIdentifier: Int,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}