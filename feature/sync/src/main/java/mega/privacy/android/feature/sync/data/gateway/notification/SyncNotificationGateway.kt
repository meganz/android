package mega.privacy.android.feature.sync.data.gateway.notification

import mega.privacy.android.data.database.entity.SyncShownNotificationEntity

/**
 * Gateway that manages the shown sync notifications
 */
internal interface SyncNotificationGateway {

    suspend fun setNotificationShown(notification: SyncShownNotificationEntity)

    suspend fun getNotificationByType(type: String): List<SyncShownNotificationEntity>

    suspend fun deleteNotificationByType(type: String)

}