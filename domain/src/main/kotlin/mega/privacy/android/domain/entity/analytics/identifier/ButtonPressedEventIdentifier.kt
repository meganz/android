package mega.privacy.android.domain.entity.analytics.identifier

/**
 * Button pressed event identifier
 *
 * @property buttonName
 * @property uniqueIdentifier
 * @property screenName
 * @property dialogName
 * @constructor Create empty Button pressed event identifier
 */
data class ButtonPressedEventIdentifier(
    val buttonName: String,
    val uniqueIdentifier: Int,
    val screenName: String?,
    val dialogName: String?,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}