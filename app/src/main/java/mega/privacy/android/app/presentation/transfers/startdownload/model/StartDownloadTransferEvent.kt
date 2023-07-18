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
     * The user have chosen to go to settings to file management (from no sufficient disk space snack bar action)
     */
    object GoToFileManagement : StartDownloadTransferEvent

    /**
     * we can't do work because there's no Internet connection
     */
    object NotConnected : StartDownloadTransferEvent

    /**
     * A message should be shown
     * @param message the [StringRes] of the message to be shown
     * @param action the [StringRes] of the action, if any
     * @param actionEvent the one off event to be triggered with the [action], if [action] is null this parameter will be ignored
     */
    sealed class Message(
        @StringRes val message: Int,
        @StringRes val action: Int? = null,
        val actionEvent: StartDownloadTransferEvent? = null,
    ) : StartDownloadTransferEvent {

        /**
         * Not sufficient space for save the node, either for offline or an ordinary download
         */
        object NotSufficientSpace : Message(
            R.string.error_not_enough_free_space,
            R.string.action_settings,
            GoToFileManagement
        )

        /**
         * Transfer cancelled by user action
         */
        object TransferCancelled : Message(R.string.transfers_cancelled, null, null)
    }
}