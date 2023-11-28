package mega.privacy.android.app.presentation.transfers.startdownload.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * One off events related to start download transfers
 */
sealed interface StartDownloadTransferEvent {

    /**
     * Transfers scanning has finished
     * @param exception [Throwable] in case of not correctly finished
     * @param totalNodes
     */
    data class FinishProcessing(val exception: Throwable?, val totalNodes: Int) :
        StartDownloadTransferEvent

    /**
     * we can't do work because there's no Internet connection
     */
    data object NotConnected : StartDownloadTransferEvent

    /**
     * User needs to confirm large download
     * @param sizeString: the size of the download
     * @param transferTriggerEvent: the event to start again the download if confirmed
     */
    data class ConfirmLargeDownload(
        val sizeString: String,
        val transferTriggerEvent: TransferTriggerEvent,
    ) : StartDownloadTransferEvent

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
    ) : StartDownloadTransferEvent {
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
    }
}