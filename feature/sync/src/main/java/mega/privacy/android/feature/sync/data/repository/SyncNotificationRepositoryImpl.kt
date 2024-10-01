package mega.privacy.android.feature.sync.data.repository

import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import javax.inject.Inject

internal class SyncNotificationRepositoryImpl @Inject constructor(
    syncNotificationGateway: SyncNotificationGateway
) : SyncNotificationRepository {

    override suspend fun isBatteryLowNotificationShown(): Boolean {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun getBatteryLowNotification(): SyncNotificationMessage {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun setBatteryLowNotificationShown(shown: Boolean) {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun isUserNotOnWifiNotificationShown(): Boolean {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun setUserNotOnWifiNotificationShown(shown: Boolean) {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun getUserNotOnWifiNotification(): SyncNotificationMessage {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun isSyncErrorsNotificationShown(syncs: List<FolderPair>): Boolean {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun setSyncErrorsNotificationShown(syncs: List<FolderPair>) {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun getSyncErrorsNotification(): SyncNotificationMessage {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun isSyncStalledIssuesNotificationShown(stalledIssues: List<StalledIssue>): Boolean {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun setSyncStalledIssuesNotificationShown(stalledIssues: List<StalledIssue>) {
        // Will be implemented in next MR
        throw NotImplementedError()
    }

    override suspend fun getSyncStalledIssuesNotification(): SyncNotificationMessage {
        // Will be implemented in next MR
        throw NotImplementedError()
    }
}