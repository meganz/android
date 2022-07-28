package mega.privacy.android.app.mediaplayer.model

import android.app.Notification
import android.app.PendingIntent
import androidx.lifecycle.LiveData
import mega.privacy.android.app.mediaplayer.service.Metadata
import java.io.File

/**
 * Class for define the parameters that created player notification
 *
 * @property notificationId notificationId
 * @property channelId channelId
 * @property channelNameResourceId channelNameResourceId
 * @property channelDescriptionResourceId channelDescriptionResourceId
 * @property metadata LiveData<Metadata>
 * @property pendingIntent pendingIntent
 * @property thumbnail LiveData<File>
 * @property smallIcon smallIcon
 * @property useChronometer useChronometer
 * @property useNextActionInCompactView useNextActionInCompactView
 * @property usePreviousActionInCompactView usePreviousActionInCompactView
 * @property onNotificationPostedCallback callback for onNotificationPosted
 */
data class PlayerNotificationCreatedParams(
    val notificationId: Int,
    val channelId: String,
    val channelNameResourceId: Int,
    val channelDescriptionResourceId: Int = 0,
    val metadata: LiveData<Metadata>,
    val pendingIntent: PendingIntent?,
    val thumbnail: LiveData<File>,
    val smallIcon: Int,
    val useChronometer: Boolean = false,
    val useNextActionInCompactView: Boolean = true,
    val usePreviousActionInCompactView: Boolean = true,
    val onNotificationPostedCallback: (
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean,
    ) -> Unit,
)
