package mega.privacy.android.app.presentation.transfers.starttransfer.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * One off events related to start transfers
 */
sealed interface StartTransferEvent {

    /**
     * Download transfers scanning has finished
     * @param exception [Throwable] in case of not correctly finished
     * @param triggerEvent the event that triggered the start of the transfers
     */
    data class FinishDownloadProcessing(
        val exception: Throwable?,
        val triggerEvent: TransferTriggerEvent,
    ) : StartTransferEvent

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
     * @param triggerEvent, the event that was initiated while the transfers are paused
     */
    data class PausedTransfers(val triggerEvent: TransferTriggerEvent) : StartTransferEvent

    /**
     * Copy offline has finished
     * @property totalFiles total files copied
     */
    data class FinishCopyOffline(
        val totalFiles: Int,
    ) : StartTransferEvent

    /**
     * Slow download preview in progress
     *
     * @param transferUniqueId Unique transfer Id of the preview that is being processed
     */
    data class SlowDownloadPreviewInProgress(val transferUniqueId: Long) :
        StartTransferEvent

    /**
     * User is in paywall over quota, so no transfer is permitted
     */
    data object PayWall : StartTransferEvent

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
        vararg val messageArgs: String,
    ) : StartTransferEvent {
        /**
         * The one off event to be triggered with the [action], if [action] is null this parameter will be ignored
         */
        sealed interface ActionEvent {
            /**
             * The user have chosen to go to settings to file management (from no sufficient disk space snack bar action)
             */
            data object GoToFileManagement : ActionEvent
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
         * Copy uri has finished
         */
        data object FinishCopyUri : Message(R.string.copy_already_downloaded, null, null)

        /**
         * Text file upload has finished
         *
         * @property isEditMode True if the file is uploaded in edit mode, false otherwise.
         * @property isCloudFile True if the file is uploaded from home page, false otherwise.
         */
        data class FailedTextFileUpload(
            val isEditMode: Boolean,
            val isCloudFile: Boolean,
        ) : Message(
            when {
                isEditMode -> R.string.file_update_failed
                isCloudFile -> R.string.text_editor_creation_error
                else -> R.string.file_creation_failed
            }, null, null
        )
    }
}