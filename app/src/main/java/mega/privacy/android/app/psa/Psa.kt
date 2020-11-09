package mega.privacy.android.app.psa

/**
 * The PSA view model class.
 *
 * @property id the id of the PSA (useful to call MegaApi::setPSA later)
 * @property title the title of the PSA
 * @property text the text of the PSA
 * @property imageUrl the URL of the image of the PSA
 * @property positiveText the text for the positive button (or an empty string)
 * @property positiveLink the link for the positive button (or an empty string)
 * @property url the URL (or an empty string), the only valid property for new format PSA
 */
data class Psa(
    val id: Int,
    val title: String,
    val text: String,
    val imageUrl: String?,
    val positiveText: String?,
    val positiveLink: String?,
    val url: String?
)
