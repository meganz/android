package mega.privacy.android.app.presentation.transfers.startdownload.model

/**
 * Represents the job in progress state
 */
sealed class StartDownloadTransferJobInProgress {

    /**
     * Sdk is processing the node to prepare the download.
     */
    object ProcessingFiles : StartDownloadTransferJobInProgress()
}