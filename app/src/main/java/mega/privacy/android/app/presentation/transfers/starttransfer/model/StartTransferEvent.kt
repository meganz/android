package mega.privacy.android.app.presentation.transfers.starttransfer.model

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * One off events related to start transfers
 */
sealed interface StartTransferEvent {

    /**
     * Download transfers scanning has finished
     * @param exception [Throwable] in case of not correctly finished
     * @param totalNodes
     * @param totalFiles total files on this nodes
     * @param totalAlreadyDownloaded total files already downloaded, so they won't be downloaded
     * @param triggerEvent the event that triggered the start of the transfers
     * @property filesToDownload
     */
    data class FinishDownloadProcessing(
        val exception: Throwable?,
        val totalNodes: Int,
        val totalFiles: Int,
        val totalAlreadyDownloaded: Int,
        val triggerEvent: TransferTriggerEvent,
    ) : StartTransferEvent {
        val filesToDownload = totalFiles - totalAlreadyDownloaded
    }

    /**
     * Upload transfers canning has finished.
     *
     * @param totalFiles total files to upload
     * @param triggerEvent the event that triggered the start of the transfers
     */
    data class FinishUploadProcessing(
        val totalFiles: Int,
        val triggerEvent: TransferTriggerEvent,
    ) : StartTransferEvent

    /**
     * we can't do work because there's no Internet connection
     */
    data object NotConnected : StartTransferEvent

    /**
     * Transfer cannot proceed because transfers' queue is paused.
     */
    data object PausedTransfers : StartTransferEvent

    /**
     * User needs to confirm large download
     * @param sizeString: the size of the download
     * @param transferTriggerEvent: the event to start again the download if confirmed
     */
    data class ConfirmLargeDownload(
        val sizeString: String,
        val transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) : StartTransferEvent

    /**
     * Depending on Android version and user settings, download destination should be asked for each new download
     * @param originalEvent original [TransferTriggerEvent.StartDownloadNode] event that triggered this event
     */
    data class AskDestination(val originalEvent: TransferTriggerEvent) :
        StartTransferEvent

    /**
     * A message should be shown
     * @param message the [StringRes] of the message to be shown
     * @param action the [StringRes] of the action, if any
     * @param actionEvent the one off event to be triggered with the [action], if [action] is null this parameter will be ignored
     */
    sealed class Message(
        @StringRes val message: Int,
        @StringRes val action: Int? = null,
        val actionEvent: ActionEvent? = null,
    ) : StartTransferEvent {
        /**
         * The one off event to be triggered with the [action], if [action] is null this parameter will be ignored
         */
        enum class ActionEvent {
            /**
             * The user have chosen to go to settings to file management (from no sufficient disk space snack bar action)
             */
            GoToFileManagement
        }

        /**
         * Not sufficient space for save the node, either for offline or an ordinary download
         */
        data object NotSufficientSpace : Message(
            R.string.error_not_enough_free_space,
            R.string.action_settings,
            ActionEvent.GoToFileManagement
        )

        /**
         * Transfer cancelled by user action
         */
        data object TransferCancelled : Message(R.string.transfers_cancelled, null, null)

        /**
         * Save offline has finished
         */
        data object FinishOffline : Message(R.string.file_available_offline, null, null)

        /**
         * Copy uri has finished
         */
        data object FinishCopyUri : Message(R.string.copy_already_downloaded, null, null)

        /**
         * Text file upload has finished
         *
         * @property isSuccess True if the upload finished with success, false otherwise.
         * @property isEditMode True if the file is uploaded in edit mode, false otherwise.
         * @property isCloudFile True if the file is uploaded from home page, false otherwise.
         */
        data class FinishTextFileUpload(
            val isSuccess: Boolean,
            val isEditMode: Boolean,
            val isCloudFile: Boolean,
        ) : Message(
            when {
                isSuccess && isEditMode -> R.string.file_updated
                isEditMode -> R.string.file_update_failed
                isSuccess && isCloudFile -> R.string.text_editor_creation_success
                isCloudFile -> R.string.text_editor_creation_error
                isSuccess -> R.string.file_created
                else -> R.string.file_creation_failed
            }, null, null
        )
    }

    /**
     * A message should be shown with plural resource
     * @param message the [PluralsRes] of the message to be shown
     * @param quantity
     */
    sealed class MessagePlural(
        @PluralsRes val message: Int,
        val quantity: Int,
    ) : StartTransferEvent {
        /**
         * Save to device has finished
         * @param totalNodes
         */
        data class FinishDownloading(val totalNodes: Int) :
            MessagePlural(R.plurals.download_complete, totalNodes)

        /**
         * Upload has finished
         * @param totalFiles
         */
        data class FinishUploading(val totalFiles: Int) :
            MessagePlural(R.plurals.upload_finish, totalFiles)
    }
}