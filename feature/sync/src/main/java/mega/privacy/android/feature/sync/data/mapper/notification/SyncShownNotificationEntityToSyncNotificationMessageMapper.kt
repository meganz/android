package mega.privacy.android.feature.sync.data.mapper.notification

import dagger.Lazy
import kotlinx.serialization.json.Json
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import javax.inject.Inject

internal class SyncShownNotificationEntityToSyncNotificationMessageMapper @Inject constructor(
    private val genericErrorToNotificationMessageMapper: GenericErrorToNotificationMessageMapper,
    private val stalledIssueToNotificationMessageMapper: StalledIssuesToNotificationMessageMapper,
    private val json: Lazy<Json>,
) {

    operator fun invoke(dbEntity: SyncShownNotificationEntity): SyncNotificationMessage {
        val notificationDetails = dbEntity.otherIdentifiers?.let {
            json.get().decodeFromString<NotificationDetails>(it)
        }

        return when (dbEntity.notificationType) {
            SyncNotificationType.STALLED_ISSUE.name -> {
                stalledIssueToNotificationMessageMapper(
                    issuePath = notificationDetails?.path ?: "",
                    issueId = notificationDetails?.issueId,
                )
            }

            else -> {
                genericErrorToNotificationMessageMapper(
                    SyncNotificationType.valueOf(dbEntity.notificationType),
                    notificationDetails?.path ?: "",
                    notificationDetails?.errorCode ?: 0,
                    notificationDetails?.formattedConflictBody,
                )
            }
        }
    }

    operator fun invoke(
        domainModel: SyncNotificationMessage,
        id: Int?,
    ): SyncShownNotificationEntity {
        return SyncShownNotificationEntity(
            notificationId = id,
            notificationType = domainModel.syncNotificationType.name,
            otherIdentifiers = getOtherIdentifiers(domainModel),
        )
    }

    private fun getOtherIdentifiers(domainModel: SyncNotificationMessage): String? {
        return when (domainModel.syncNotificationType) {
            SyncNotificationType.STALLED_ISSUE,
            SyncNotificationType.ERROR,
                -> {
                json.get().encodeToString(domainModel.notificationDetails)
            }

            SyncNotificationType.CROSS_DEVICE_CONFLICT -> {
                json.get().encodeToString(domainModel.notificationDetails)
            }

            else -> null
        }
    }
}
