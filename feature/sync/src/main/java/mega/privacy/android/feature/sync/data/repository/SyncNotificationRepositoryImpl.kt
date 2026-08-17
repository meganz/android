package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
import mega.privacy.android.feature.sync.data.mapper.notification.CrossDeviceConflictNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.GenericErrorToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.StalledIssuesToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.SyncShownNotificationEntityToSyncNotificationMessageMapper
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import javax.inject.Inject

internal class SyncNotificationRepositoryImpl @Inject constructor(
    private val syncNotificationGateway: SyncNotificationGateway,
    private val stalledIssuesToNotificationMessageMapper: StalledIssuesToNotificationMessageMapper,
    private val genericErrorToNotificationMessageMapper: GenericErrorToNotificationMessageMapper,
    private val syncShownNotificationEntityToSyncNotificationMessageMapper: SyncShownNotificationEntityToSyncNotificationMessageMapper,
    private val crossDeviceConflictNotificationMessageMapper: CrossDeviceConflictNotificationMessageMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncNotificationRepository {

    override suspend fun getDisplayedNotificationsByType(type: SyncNotificationType): List<SyncNotificationMessage> =
        withContext(ioDispatcher) {
            syncNotificationGateway.getNotificationByType(type.name)
                .filter { it.notificationId != null }
                .map { syncShownNotificationEntityToSyncNotificationMessageMapper(it) }
        }

    override suspend fun setDisplayedNotification(
        notification: SyncNotificationMessage,
        notificationId: Int?,
    ) {
        withContext(ioDispatcher) {
            syncNotificationGateway.setNotificationShown(
                syncShownNotificationEntityToSyncNotificationMessageMapper(
                    domainModel = notification,
                    id = notificationId,
                )
            )
        }
    }

    override suspend fun isNotificationDisplayed(notification: SyncNotificationMessage): Boolean =
        withContext(ioDispatcher) {
            syncNotificationGateway.getNotificationByType(notification.syncNotificationType.name)
                .asSequence()
                .filter { it.notificationId != null }
                .map { syncShownNotificationEntityToSyncNotificationMessageMapper(it) }
                .any { it.hasSameIdentityAs(notification) }
        }

    override suspend fun deleteDisplayedNotificationByType(type: SyncNotificationType) {
        withContext(ioDispatcher) {
            syncNotificationGateway.deleteNotificationByType(type.name)
        }
    }

    override suspend fun getBatteryLowNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper(SyncNotificationType.BATTERY_LOW)

    override suspend fun getDeviceIsNotChargingNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper(SyncNotificationType.NOT_CHARGING)

    override suspend fun getUserNotOnWifiNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper(SyncNotificationType.NOT_CONNECTED_TO_WIFI)

    override suspend fun getSyncErrorsNotification(syncsWithErrors: List<FolderPair>): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper(
            SyncNotificationType.ERROR,
            errorCode = syncsWithErrors.first().syncError?.ordinal ?: 0,
            issuePath = syncsWithErrors.first().localFolderPath
        )

    override suspend fun getSyncStalledIssuesNotification(syncsWithStalledIssues: List<StalledIssue>): SyncNotificationMessage =
        stalledIssuesToNotificationMessageMapper(
            issuePath = syncsWithStalledIssues.first()
                .let { it.localPaths.firstOrNull() ?: it.nodeNames.first() },
            issueId = syncsWithStalledIssues.first().id,
        )

    override suspend fun getDisplayedNotificationsIdsByType(type: SyncNotificationType): List<Int> =
        withContext(ioDispatcher) {
            syncNotificationGateway.getNotificationByType(type.name)
                .mapNotNull { it.notificationId }
        }

    override fun getSyncIssueNotificationByType(type: SyncNotificationType): SyncNotificationMessage {
        return genericErrorToNotificationMessageMapper(type)
    }

    override suspend fun getCrossDeviceConflictNotification(
        conflictingSyncs: List<FolderPair>,
        folderUsageResult: FolderUsageResult,
    ): SyncNotificationMessage {
        val first = conflictingSyncs.first()
        return crossDeviceConflictNotificationMessageMapper(first, folderUsageResult)
    }

    override suspend fun setPendingCrossDeviceConflictNotification(
        conflictingSyncs: List<FolderPair>,
        folderUsageResult: FolderUsageResult,
    ) {
        if (conflictingSyncs.isNotEmpty()) {
            val notification =
                getCrossDeviceConflictNotification(conflictingSyncs, folderUsageResult)
            withContext(ioDispatcher) {
                syncNotificationGateway.deletePendingNotificationByType(
                    SyncNotificationType.CROSS_DEVICE_CONFLICT.name
                )
                syncNotificationGateway.setNotificationShown(
                    syncShownNotificationEntityToSyncNotificationMessageMapper(
                        domainModel = notification,
                        id = null,
                    )
                )
            }
        }
    }

    override suspend fun clearPendingCrossDeviceConflictNotification() {
        withContext(ioDispatcher) {
            syncNotificationGateway.deletePendingNotificationByType(
                SyncNotificationType.CROSS_DEVICE_CONFLICT.name
            )
        }
    }

    override suspend fun getPendingCrossDeviceConflictNotification(): SyncNotificationMessage? {
        return withContext(ioDispatcher) {
            syncNotificationGateway
                .getPendingNotificationByType(SyncNotificationType.CROSS_DEVICE_CONFLICT.name)
                .firstOrNull()
                ?.let { syncShownNotificationEntityToSyncNotificationMessageMapper(it) }
        }
    }

    private fun SyncNotificationMessage.hasSameIdentityAs(other: SyncNotificationMessage): Boolean {
        if (syncNotificationType != other.syncNotificationType) return false
        return when (syncNotificationType) {
            SyncNotificationType.ERROR,
            SyncNotificationType.CROSS_DEVICE_CONFLICT,
                -> notificationDetails.path.equals(other.notificationDetails.path, ignoreCase = true)

            SyncNotificationType.STALLED_ISSUE -> {
                val issueId = notificationDetails.issueId
                val otherIssueId = other.notificationDetails.issueId
                if (issueId != null && otherIssueId != null) {
                    issueId == otherIssueId
                } else {
                    notificationDetails.path.equals(other.notificationDetails.path, ignoreCase = true)
                }
            }

            else -> true
        }
    }
}
