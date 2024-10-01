package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Result of [GetFileDestinationAndAppDataForDownloadUseCase]
 * @param folderDestination UriPath that represents the folder path to start the download
 * @param appData [TransferAppData.SdCardDownload] if needed
 */
data class DestinationAndAppDataForDownloadResult(
    val folderDestination: UriPath,
    val appData: TransferAppData.SdCardDownload?,
)