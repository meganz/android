package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Sync shown notification entity for Room database
 *
 * @property id - Room database entity id
 * @property notificationId - the id of the displayed notification
 * @property notificationType - the type of notification (batteryLow, userNotOnWifi, syncErrors, syncStalledIssues)
 * @property otherIdentifiers - other identifiers for the notification such as stalled issue path, sync id, etc (optional)
 */
@Entity(
    tableName = MegaDatabaseConstant.TABLE_SYNC_SHOWN_NOTIFICATIONS
)
data class SyncShownNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "notificationId")
    val notificationId: Int? = null,
    @ColumnInfo(name = "notificationType")
    val notificationType: String,
    @ColumnInfo(name = "otherIdentifiers")
    val otherIdentifiers: String? = null,
)
