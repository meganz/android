package mega.privacy.android.app.presentation.transfers.starttransfer.model

/**
 * Represents the job in progress state
 */
sealed class StartTransferJobInProgress {

    /**
     * Sdk is scanning the transfers
     */
    data object ScanningTransfers : StartTransferJobInProgress()
}