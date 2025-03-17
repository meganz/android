package mega.privacy.android.domain.entity.banner

/**
 * Banner
 * @property id Banner id
 * @property title Banner title
 * @property description Banner description
 * @property image Banner image
 * @property backgroundImage Banner background image
 * @property url Banner url
 * @property imageLocation Banner image location
 */
data class Banner(
    val id: Int,
    val title: String,
    val description: String,
    val image: String,
    val backgroundImage: String,
    val url: String,
    val imageLocation: String,
)