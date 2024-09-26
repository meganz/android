package mega.privacy.android.feature.sync.domain.entity

/**
 * Class to define the type of sync notification.
 *
 * @property title The title of the notification.
 * @property text The text of the notification.
 * @property syncNotificationType The type of the notification.
 * @property path The path of the folder that caused the notification.
 * @property errorCode The error code of the notification.
 */
data class SyncNotificationMessage(
    val title: String,
    val text: String,
    val syncNotificationType: SyncNotificationType,
    val path: String?,
    val errorCode: Int?,
)