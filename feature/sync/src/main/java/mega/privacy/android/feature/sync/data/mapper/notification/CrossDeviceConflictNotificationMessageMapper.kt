package mega.privacy.android.feature.sync.data.mapper.notification

import mega.privacy.android.domain.entity.node.FolderUsageResult
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.ui.formatter.FolderConflictMessageFormatter
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import javax.inject.Inject

internal class CrossDeviceConflictNotificationMessageMapper @Inject constructor(
    private val folderConflictMessageFormatter: FolderConflictMessageFormatter,
) {

    operator fun invoke(
        folderPair: FolderPair,
        folderUsage: FolderUsageResult,
    ): SyncNotificationMessage {
        val body = folderConflictMessageFormatter.formatFromFolderUsage(
            folderDisplayName = folderPair.remoteFolder.name,
            folderTypeLabelRes = sharedResR.string.sync_label_cloud_folder,
            result = folderUsage,
        )
        if (body == null) {
            Timber.e("CrossDeviceConflictNotificationMessageMapper called with NotUsed folder usage for pair: ${folderPair.pairName}")
        }
        return SyncNotificationMessage(
            title = sharedResR.string.sync_snackbar_message_confirm_sync_stopped,
            text = sharedResR.string.general_sync_notification_generic_error_text,
            syncNotificationType = SyncNotificationType.CROSS_DEVICE_CONFLICT,
            notificationDetails = NotificationDetails(
                path = folderPair.pairName,
                errorCode = 0,
                formattedConflictBody = body,
            ),
            formattedText = body,
        )
    }
}
