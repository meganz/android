package mega.privacy.android.feature.sync.data.mapper.notification

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import javax.inject.Inject

internal class SyncShownNotificationEntityToSyncNotificationMessageMapper @Inject constructor(
    private val genericErrorToNotificationMessageMapper: GenericErrorToNotificationMessageMapper,
    private val stalledIssueToNotificationMessageMapper: StalledIssuesToNotificationMessageMapper,
    private val json: Json,
) {

    operator fun invoke(dbEntity: SyncShownNotificationEntity): SyncNotificationMessage {
        val notificationDetails = dbEntity.otherIdentifiers?.let {
            json.decodeFromString<NotificationDetails>(it)
        }

        return when (dbEntity.notificationType) {
            SyncNotificationType.STALLED_ISSUE.name -> {
                stalledIssueToNotificationMessageMapper(
                    issuePath = notificationDetails?.path ?: ""
                )
            }

            else -> {
                genericErrorToNotificationMessageMapper(
                    SyncNotificationType.valueOf(dbEntity.notificationType),
                    notificationDetails?.path ?: "",
                    notificationDetails?.errorCode ?: 0
                )
            }
        }
    }

    operator fun invoke(domainModel: SyncNotificationMessage): SyncShownNotificationEntity {
        return SyncShownNotificationEntity(
            notificationType = domainModel.syncNotificationType.name,
            otherIdentifiers = getOtherIdentifiers(domainModel),
        )
    }

    private fun getOtherIdentifiers(domainModel: SyncNotificationMessage): String? {
        return when (domainModel.syncNotificationType) {
            SyncNotificationType.STALLED_ISSUE,
            SyncNotificationType.ERROR,
            -> {
                json.encodeToString(domainModel.notificationDetails)
            }

            else -> null
        }
    }
}