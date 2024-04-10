package mega.privacy.android.app.presentation.transfers.startdownload.model

/**
 * Represents the job in progress state
 */
sealed class StartDownloadTransferJobInProgress {

    /**
     * Sdk is scanning the transfers
     */
    data object ScanningTransfers : StartDownloadTransferJobInProgress()
}