package mega.privacy.android.domain.entity.analytics.identifier

/**
 * Screen view event identifier
 *
 * @property name
 * @property uniqueIdentifier
 */
data class ScreenViewEventIdentifier(
    val name: String,
    val uniqueIdentifier: Int,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}