package mega.privacy.android.domain.entity.banner


/**
 * PromoBanner
 * @property id  id
 * @property title  title
 * @property buttonText  button text
 * @property image  image
 * @property backgroundImage  background image
 * @property url  url
 * @property imageLocation image location
 */
data class PromotionalBanner(
    val id: Int,
    val title: String,
    val buttonText: String,
    val image: String,
    val backgroundImage: String,
    val url: String,
    val imageLocation: String,
)
