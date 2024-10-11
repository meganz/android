package mega.privacy.android.feature.sync.data.gateway.notification

import dagger.Lazy
import mega.privacy.android.data.database.dao.SyncShownNotificationDao
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import javax.inject.Inject

internal class SyncNotificationGatewayImpl @Inject constructor(
    private val syncShownNotificationDao: Lazy<SyncShownNotificationDao>
) : SyncNotificationGateway {

    override suspend fun setNotificationShown(notification: SyncShownNotificationEntity) {
        syncShownNotificationDao.get().insertSyncNotification(notification)
    }

    override suspend fun getNotificationByType(type: String): List<SyncShownNotificationEntity> =
        syncShownNotificationDao.get().getSyncNotificationByType(type)

    override suspend fun deleteNotificationByType(type: String) {
        syncShownNotificationDao.get().deleteSyncNotificationByType(type)
    }
}