package mega.privacy.android.feature.sync.domain.entity

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

/**
 * Class to define the type of sync notification.
 *
 * @property title The title of the notification.
 * @property text The text of the notification.
 * @property syncNotificationType The type of the notification.
 * @property notificationDetails Additional details
 * containing error code and the path of the folder that caused the notification.
 */
data class SyncNotificationMessage(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val syncNotificationType: SyncNotificationType,
    val notificationDetails: NotificationDetails,
)

/**
 * Class to define the type of sync notification.
 * @property path The path of the folder that caused the notification.
 * @property errorCode The error code of the notification.
 */
@Serializable
data class NotificationDetails(
    val path: String?,
    val errorCode: Int?,
)