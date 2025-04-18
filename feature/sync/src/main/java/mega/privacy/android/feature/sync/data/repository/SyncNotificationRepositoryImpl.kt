package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncNotificationRepository {

    override suspend fun getDisplayedNotificationsByType(type: SyncNotificationType): List<SyncNotificationMessage> =
        withContext(ioDispatcher) {
            syncNotificationGateway.getNotificationByType(type.name)
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

    override suspend fun deleteDisplayedNotificationByType(type: SyncNotificationType) {
        withContext(ioDispatcher) {
            syncNotificationGateway.deleteNotificationByType(type.name)
        }
    }

    override suspend fun getBatteryLowNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper(SyncNotificationType.BATTERY_LOW)

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
                .let { it.localPaths.firstOrNull() ?: it.nodeNames.first() })

    override suspend fun getDisplayedNotificationsIdsByType(type: SyncNotificationType): List<Int> =
        withContext(ioDispatcher) {
            syncNotificationGateway.getNotificationByType(type.name)
                .mapNotNull { it.notificationId }
        }

    override fun getSyncIssueNotificationByType(type: SyncNotificationType): SyncNotificationMessage {
        return genericErrorToNotificationMessageMapper(type)
    }
}
