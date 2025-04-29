package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import javax.inject.Inject

/**
 * Gets the title for ActionGroup finish Notifications
 */
class ActionGroupFinishNotificationTitleMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resources get() = context.resources

    /**
     * Invoke
     */
    operator fun invoke(
        isDownload: Boolean,
        isPreviewDownload: Boolean,
        titleSuffix: String?,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ): String {
        val totalCompleted = actionGroup.completedFiles
        val totalFinished = actionGroup.finishedFiles
        val notificationTitle = when {
            totalCompleted != totalFinished -> {
                resources.getQuantityString(
                    if (isDownload) {
                        R.plurals.download_service_final_notification_with_details
                    } else {
                        R.plurals.upload_service_final_notification
                    },
                    totalFinished,
                    totalCompleted,
                    totalFinished,
                )
            }

            totalCompleted > 1 || actionGroup.singleFileName == null -> {
                resources.getQuantityString(
                    if (isDownload) {
                        R.plurals.download_service_final_notification
                    } else {
                        R.plurals.upload_service_final_notification
                    },
                    totalCompleted,
                    totalCompleted
                )
            }

            isPreviewDownload -> {
                resources.getString(
                    mega.privacy.android.shared.resources.R.string.transfers_notification_title_preview_download,
                    actionGroup.singleFileName
                )
            }

            else -> {
                resources.getString(
                    if (isDownload) {
                        mega.privacy.android.shared.resources.R.string.transfers_notification_title_single_download
                    } else {
                        mega.privacy.android.shared.resources.R.string.transfers_notification_title_single_upload
                    },
                    actionGroup.singleFileName
                )
            }
        } + (titleSuffix?.let { ". $it." } ?: "")
        return notificationTitle
    }
}