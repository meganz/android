package mega.privacy.android.app.presentation.transfers.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.data.worker.AbstractTransfersWorker.Companion.finalSummaryGroup
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsContentUriUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Default implementation of [TransfersActionGroupFinishNotificationBuilder]
 */
class DefaultTransfersActionGroupFinishNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isContentUriUseCase: IsContentUriUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val actionGroupFinishNotificationActionTextMapper: ActionGroupFinishNotificationActionTextMapper,
    private val actionGroupFinishNotificationTitleMapper: ActionGroupFinishNotificationTitleMapper,
    private val openTransfersSectionIntentMapper: OpenTransfersSectionIntentMapper,
) : TransfersActionGroupFinishNotificationBuilder {
    private val resources get() = context.resources
    override suspend fun invoke(
        actionGroup: ActiveTransferTotals.ActionGroup,
        transferType: TransferType,
    ): Notification {
        if (transferType != TransferType.GENERAL_UPLOAD && transferType != TransferType.DOWNLOAD) {
            throw NotImplementedError("Group notifications are not yet implemented for this type: $transferType")
        }
        val isDownload = transferType == TransferType.DOWNLOAD
        val isPreviewDownload = isDownload && actionGroup.isPreviewDownload()
        val isOfflineDownload = isDownload && actionGroup.isOfflineDownload()
        val isLoggedIn = isUserLoggedInUseCase()
        val titleSuffix = titleSuffix(
            isDownload = isDownload,
            actionGroup = actionGroup,
        )
        val notificationTitle = actionGroupFinishNotificationTitleMapper(
            isDownload = isDownload,
            isPreviewDownload = isPreviewDownload,
            titleSuffix = titleSuffix,
            actionGroup = actionGroup,
        )

        val contentText = contentText(
            isPreviewDownload = isPreviewDownload,
            isOfflineDownload = isOfflineDownload,
            titleSuffix = titleSuffix,
            actionGroup = actionGroup,
        )

        val actionText = actionGroupFinishNotificationActionTextMapper(
            isLoggedIn = isLoggedIn,
            isPreviewDownload = isPreviewDownload,
            isOfflineDownload = isOfflineDownload,
            actionGroup = actionGroup,
        )

        val actionIntent = actionIntent(
            isLoggedIn = isLoggedIn,
            isDownload = isDownload,
            isPreviewDownload = isPreviewDownload,
            isOfflineDownload = isOfflineDownload,
            actionGroup = actionGroup,
        )
        val contentIntent =
            if (isPreviewDownload || actionGroup.groupId < 0) { //not a real transfer, will not appear on transfer section -> content intent same as action intent
                actionIntent
            } else {
                openTransfersSectionIntentMapper(TransfersTab.COMPLETED_TAB)
            }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis()
                .toInt(), // Unique request code to make sure old intents are not reused
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
            .setSmallIcon(iconPackR.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.red_600_red_300))
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
            .setTicker(notificationTitle)
            .setContentTitle(notificationTitle)
            .setOngoing(false)
            .apply {
                contentText?.let {
                    setContentText(it)
                }
                actionText?.let {
                    addAction(
                        iconPackR.drawable.ic_stat_notify,
                        actionText,
                        actionPendingIntent
                    )
                }
            }
            .setGroup(finalSummaryGroup(transferType))
            .build()
    }

    /**
     * String to be added at the end of the title and content text if there are errors or already transferred files
     */
    private fun titleSuffix(
        isDownload: Boolean,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ): String? {
        val alreadyTransferredCount = actionGroup.alreadyTransferred
        val errorCount = actionGroup.finishedFilesWithErrors
        val titleSuffix = when {
            errorCount > 0 && alreadyTransferredCount > 0 -> {
                "${alreadyMsg(alreadyTransferredCount, isDownload)}, " +
                        errorMsg(errorCount, isDownload)
            }

            errorCount > 0 -> errorMsg(errorCount, isDownload)
            alreadyTransferredCount > 0 -> alreadyMsg(alreadyTransferredCount, isDownload)
            else -> null
        }
        return titleSuffix
    }

    private suspend fun contentText(
        isPreviewDownload: Boolean,
        isOfflineDownload: Boolean,
        titleSuffix: String?,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ): String? {
        val destinationText = when {
            isOfflineDownload -> context.getString(R.string.section_saved_for_offline_new)
            isPreviewDownload -> null
            else -> runCatching {
                if (isContentUriUseCase(actionGroup.destination)) {
                    getPathByDocumentContentUriUseCase(actionGroup.destination)
                } else {
                    actionGroup.destination
                }
            }.getOrNull() ?: actionGroup.destination
        }
        val contentText = destinationText?.let {
            resources.getString(
                sharedR.string.transfers_notification_location_content,
                it,
            )
        }?.let {
            if (titleSuffix == null) {
                it + "\n" + resources.getString(
                    R.string.general_total_size,
                    fileSizeStringMapper(actionGroup.totalBytes)
                )
            } else {
                it
            }
        }
        return contentText
    }

    private fun actionIntent(
        isLoggedIn: Boolean,
        isDownload: Boolean,
        isPreviewDownload: Boolean,
        isOfflineDownload: Boolean,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ) = if (isPreviewDownload) {
        previewIntent(actionGroup)
    } else {
        actionIntent(
            isLoggedIn = isLoggedIn,
            isDownload = isDownload,
            isOfflineDownload = isOfflineDownload,
            actionGroup = actionGroup
        )
    }

    private fun previewIntent(
        actionGroup: ActiveTransferTotals.ActionGroup,
    ): Intent {
        val previewFile = actionGroup.singleFileName?.let {
            File(actionGroup.destination + actionGroup.singleFileName)
        }
        val previewIntent: Intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val type = previewFile?.let { fileTypeInfoMapper(it.name) }
        val isZipFile = type is ZipFileTypeInfo && runCatching { ZipFile(previewFile) }.isSuccess
        val uri = if (previewFile?.exists() == true && !isZipFile) {
            runCatching {
                FileProvider.getUriForFile(
                    context,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    previewFile
                )
            }.getOrNull()
        } else null
        return when {
            previewFile?.exists() == true && isZipFile -> {
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_EXPLORE_ZIP
                    putExtra(Constants.EXTRA_PATH_ZIP, previewFile.absolutePath)
                }
            }

            uri != null && MegaApiUtils.isIntentAvailable(context, previewIntent) -> {
                previewIntent.setDataAndType(uri, type?.mimeType)
                val chooserTitle = resources.getString(
                    sharedR.string.open_with_os_dialog_title,
                    actionGroup.singleFileName
                )
                Intent.createChooser(previewIntent, chooserTitle)
            }

            else -> {
                val warningMessage = resources.getString(R.string.intent_not_available)
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_SHOW_WARNING
                    putExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE, warningMessage)
                }
            }
        }
    }

    private fun actionIntent(
        isLoggedIn: Boolean,
        isDownload: Boolean,
        isOfflineDownload: Boolean,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ): Intent = when {
        isDownload -> {
            Intent(
                context,
                if (isLoggedIn) ManagerActivity::class.java else FileStorageActivity::class.java
            ).apply {
                if (isLoggedIn) {
                    action = Constants.ACTION_LOCATE_DOWNLOADED_FILE
                    putExtra(Constants.INTENT_EXTRA_IS_OFFLINE_PATH, isOfflineDownload)
                } else {
                    action = FileStorageActivity.Mode.BROWSE_FILES.action
                }
                putExtra(FileStorageActivity.EXTRA_PATH, actionGroup.destination)
                putStringArrayListExtra(
                    FileStorageActivity.EXTRA_FILE_NAMES,
                    ArrayList(actionGroup.fileNames)
                )
            }
        }

        else -> { // is not download
            Intent(context, ManagerActivity::class.java).apply {
                action = Constants.ACTION_OPEN_FOLDER
                putExtra(
                    Constants.INTENT_EXTRA_KEY_PARENT_HANDLE,
                    actionGroup.pendingTransferNodeId?.nodeId?.longValue
                )
                putStringArrayListExtra(
                    FileStorageActivity.EXTRA_FILE_NAMES,
                    ArrayList(actionGroup.fileNames)
                )
            }
        }
    }

    private fun errorMsg(errorCount: Int, isDownload: Boolean) = resources.getQuantityString(
        if (isDownload) {
            R.plurals.download_service_failed
        } else {
            R.plurals.upload_service_failed
        },
        errorCount,
        errorCount
    )

    private fun alreadyMsg(alreadyDownloadedCount: Int, isDownload: Boolean) =
        resources.getQuantityString(
            if (isDownload) {
                R.plurals.already_downloaded_service
            } else {
                R.plurals.upload_service_notification_already_uploaded
            },
            alreadyDownloadedCount,
            alreadyDownloadedCount,
        )
}