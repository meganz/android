package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
import mega.privacy.android.feature.sync.data.mapper.notification.GenericErrorToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.StalledIssuesToNotificationMessageMapper
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SyncNotificationRepository {

    override suspend fun isBatteryLowNotificationShown(): Boolean =
        withContext(ioDispatcher) {
            syncNotificationGateway
                .getNotificationByType(SyncNotificationType.BATTERY_LOW.name)
                .isNotEmpty()
        }

    override suspend fun getBatteryLowNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper()

    override suspend fun setBatteryLowNotificationShown(shown: Boolean) {
        withContext(ioDispatcher) {
            if (shown) {
                syncNotificationGateway.setNotificationShown(
                    SyncShownNotificationEntity(
                        notificationType = SyncNotificationType.BATTERY_LOW.name
                    )
                )
            } else {
                syncNotificationGateway.deleteNotificationByType(SyncNotificationType.BATTERY_LOW.name)
            }
        }
    }

    override suspend fun isUserNotOnWifiNotificationShown(): Boolean =
        withContext(ioDispatcher) {
            syncNotificationGateway
                .getNotificationByType(SyncNotificationType.NOT_CONNECTED_TO_WIFI.name)
                .isNotEmpty()
        }

    override suspend fun setUserNotOnWifiNotificationShown(shown: Boolean) {
        withContext(ioDispatcher) {
            if (shown) {
                syncNotificationGateway.setNotificationShown(
                    SyncShownNotificationEntity(
                        notificationType = SyncNotificationType.NOT_CONNECTED_TO_WIFI.name
                    )
                )
            } else {
                syncNotificationGateway.deleteNotificationByType(SyncNotificationType.NOT_CONNECTED_TO_WIFI.name)
            }
        }
    }

    override suspend fun getUserNotOnWifiNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper()

    override suspend fun isSyncErrorsNotificationShown(syncs: List<FolderPair>): Boolean =
        withContext(ioDispatcher) {
            syncNotificationGateway
                .getNotificationByType(SyncNotificationType.ERROR.name)
                .isNotEmpty()
        }

    override suspend fun setSyncErrorsNotificationShown(
        syncs: List<FolderPair>,
        shown: Boolean,
    ) {
        withContext(ioDispatcher) {
            if (shown && syncs.isNotEmpty()) {
                syncNotificationGateway.setNotificationShown(
                    SyncShownNotificationEntity(
                        notificationType = SyncNotificationType.ERROR.name,
                        otherIdentifiers = null  // identification for each stalled issue will be added in next MR
                    )
                )
            } else {
                syncNotificationGateway.deleteNotificationByType(SyncNotificationType.ERROR.name)
            }
        }
    }

    override suspend fun getSyncErrorsNotification(): SyncNotificationMessage =
        genericErrorToNotificationMessageMapper()

    override suspend fun isSyncStalledIssuesNotificationShown(stalledIssues: List<StalledIssue>): Boolean =
        withContext(ioDispatcher) {
            syncNotificationGateway
                .getNotificationByType(SyncNotificationType.STALLED_ISSUE.name)
                .isNotEmpty()
        }

    override suspend fun setSyncStalledIssuesNotificationShown(
        stalledIssues: List<StalledIssue>,
        shown: Boolean,
    ) {
        withContext(ioDispatcher) {
            if (shown && stalledIssues.isNotEmpty()) {
                syncNotificationGateway.setNotificationShown(
                    SyncShownNotificationEntity(
                        notificationType = SyncNotificationType.STALLED_ISSUE.name,
                        otherIdentifiers = null // identification for each stalled issue will be added in next MR
                    )
                )
            } else {
                syncNotificationGateway.deleteNotificationByType(SyncNotificationType.STALLED_ISSUE.name)
            }
        }
    }

    override suspend fun getSyncStalledIssuesNotification(): SyncNotificationMessage =
        stalledIssuesToNotificationMessageMapper()
}