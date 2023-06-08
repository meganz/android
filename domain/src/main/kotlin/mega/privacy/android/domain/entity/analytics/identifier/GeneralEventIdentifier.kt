package mega.privacy.android.domain.entity.analytics.identifier

/**
 * General event identifier
 *
 * @property name
 * @property info
 * @property uniqueIdentifier
 * @constructor Create empty General event identifier
 */
data class GeneralEventIdentifier(
    val name: String,
    val info: String?,
    val uniqueIdentifier: Int,
) {
    init {
        require(uniqueIdentifier in 0..999) { "UniqueIdentifier has to be in the range of 0 to 999" }
    }
}