package mega.privacy.android.app.presentation.transfers.starttransfer.model

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.shared.resources.R as sharedR

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
     * @param transferPath Path where the preview is being downloaded
     */
    data class SlowDownloadPreviewInProgress(val transferUniqueId: Long, val transferPath: String) :
        StartTransferEvent

    /**
     * User is in paywall over quota, so no transfer is permitted
     */
    data object PayWall : StartTransferEvent

    /**
     * A snackbar message should be shown
     */
    sealed interface Message : StartTransferEvent {
        /**
         * [StringRes] of the action, if any
         */
        val action: Int?

        /**
         * the one off event to be triggered with the [action], if [action] is null this parameter will be ignored
         */
        val actionEvent: ActionEvent?

        /**
         * The message to be shown in the snackbar
         */
        fun getMessage(context: Context): String

        /**
         * @param messageRes the [StringRes] of the message to be shown
         * @param messageArgs arguments to build the message, if needed
         */
        sealed class MessageStringRes(
            @StringRes val messageRes: Int,
            @StringRes override val action: Int? = null,
            override val actionEvent: ActionEvent? = null,
            private vararg val messageArgs: String,
        ) : Message {
            override fun getMessage(context: Context) =
                context.getString(messageRes, *messageArgs)
        }

        /**
         * @param pluralRes the [StringRes] of the message to be shown
         * @param messageArgs arguments to build the message, if needed
         */
        sealed class MessagePluralRes(
            @PluralsRes val pluralRes: Int,
            val amount: Int,
            @StringRes override val action: Int? = null,
            override val actionEvent: ActionEvent? = null,
            private vararg val messageArgs: String,
        ) : Message {
            override fun getMessage(context: Context) =
                context.resources.getQuantityString(pluralRes, amount, *messageArgs)
        }

        /**
         * The one off event to be triggered with the [action], if [action] is null this parameter will be ignored
         */
        sealed interface ActionEvent {

            /**
             * Retry a failed transfer that has already been retried but failed again
             * @param transferTriggerEvent the original retry event that will be re-retied
             */
            data class ReRetry(
                val transferTriggerEvent: TransferTriggerEvent.RetryTransfers,
            ) : ActionEvent
        }

        /**
         * Not sufficient space for save the node, either for offline or an ordinary download
         */
        data object NotSufficientSpace : MessageStringRes(R.string.error_not_enough_free_space)

        /**
         * Transfer cancelled by user action
         */
        data object TransferCancelled : MessageStringRes(R.string.transfers_cancelled)

        /**
         * Copy uri has finished
         */
        data object FinishCopyUri : MessageStringRes(R.string.copy_already_downloaded)

        /**
         * Text file upload has finished
         *
         * @property isEditMode True if the file is uploaded in edit mode, false otherwise.
         * @property isCloudFile True if the file is uploaded from home page, false otherwise.
         */
        data class FailedTextFileUpload(
            val isEditMode: Boolean,
            val isCloudFile: Boolean,
        ) : MessageStringRes(
            when {
                isEditMode -> R.string.file_update_failed
                isCloudFile -> R.string.text_editor_creation_error
                else -> R.string.file_creation_failed
            }
        )

        /**
         * Message to be shown when failed transfers are retried with success
         * @param retryAmount
         */
        data class TransfersRetriedSucceed(val retryAmount: Int) : MessageStringRes(
            if (retryAmount == 1) {
                sharedR.string.transfers_retry_feedback_snackbar_message_singular
            } else {
                sharedR.string.transfers_retry_feedback_snackbar_message_plural
            }
        )

        /**
         * Message to be shown when failed transfers are retried without success
         * @param transferTriggerEvent the original event to be re-retried if message action is triggered
         */
        data class TransfersRetriedFailed(val transferTriggerEvent: TransferTriggerEvent.RetryTransfers) :
            MessageStringRes(
                messageRes = sharedR.string.transfers_retry_failed_snackbar_message,
                action = sharedR.string.transfers_retry_failed_snackbar_action,
                actionEvent = ActionEvent.ReRetry(transferTriggerEvent)
            )
    }
}