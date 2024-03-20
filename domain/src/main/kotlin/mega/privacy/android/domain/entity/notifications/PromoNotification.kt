package mega.privacy.android.domain.entity.notifications

/**
 * Promo notification
 *
 * @property promoID         ID of the promotional notification (it's created in ascending order)
 * @property title           Title of the promotional notification
 * @property description     Description of the promotional notification
 * @property imageName       Name of the image file
 * @property imageURL        URL of the image file, the URL points to one of our static servers
 * @property startTimeStamp  Start time of the promotional notification, when it became available to the user
 * @property endTimeStamp    End time of the promotional notification, when it will become expired
 * @property actionName      Name of the action to be performed when the user taps the promotional notification
 * @property actionURL       URL of the action to be performed when the user taps the promotional notification
 */
data class PromoNotification(
    val promoID: Long,
    val title: String,
    val description: String,
    val imageName: String,
    val imageURL: String,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
    val actionName: String,
    val actionURL: String,
)
