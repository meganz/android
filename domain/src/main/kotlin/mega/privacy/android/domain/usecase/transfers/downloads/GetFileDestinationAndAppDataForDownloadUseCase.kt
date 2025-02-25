package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsExternalStorageContentUriUseCase
import javax.inject.Inject

/**
 * Use case to get the destination and app data to be used to start a download given a desired destination
 * The returned destination is a file path that can be send to the SDK, app data is set to save the desired destination if needed.
 */
class GetFileDestinationAndAppDataForDownloadUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val transferRepository: TransferRepository,
    private val cacheRepository: CacheRepository,
    private val isExternalStorageContentUriUseCase: IsExternalStorageContentUriUseCase,
    private val getExternalPathByContentUriUseCase: GetExternalPathByContentUriUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     * Invoke
     * @param uriPathFolderDestination the uriPath that represents the download destination, it can be a content uri or a file path
     */
    suspend operator fun invoke(uriPathFolderDestination: UriPath): DestinationAndAppDataForDownloadResult {
        val downloadDestination: String?
        val folderDestination: UriPath?
        val appData: TransferAppData?
        when {
            fileSystemRepository.isSDCardPathOrUri(uriPathFolderDestination.value) -> {
                if (getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination)) {
                    appData = null
                    folderDestination = uriPathFolderDestination
                } else {
                    downloadDestination = transferRepository.getOrCreateSDCardTransfersCacheFolder()
                        ?.path
                    folderDestination = downloadDestination?.let { UriPath(it) }
                    appData =
                        TransferAppData.SdCardDownload(
                            targetPathForSDK = downloadDestination ?: "",
                            finalTargetUri = uriPathFolderDestination.value
                        )
                }
            }

            isExternalStorageContentUriUseCase(uriPathFolderDestination.value) -> {
                appData = null
                folderDestination =
                    if (getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination)) {
                        uriPathFolderDestination
                    } else {
                        getExternalPathByContentUriUseCase(uriPathFolderDestination.value)?.let {
                            UriPath(it)
                        }
                    }
            }

            fileSystemRepository.isContentUri(uriPathFolderDestination.value) -> {
                downloadDestination = cacheRepository.getCacheFolder(
                    cacheRepository.getCacheFolderNameForTransfer(false)
                )?.path
                folderDestination = downloadDestination?.let { UriPath(it) }
                appData =
                    TransferAppData.SdCardDownload(
                        targetPathForSDK = downloadDestination ?: "",
                        finalTargetUri = uriPathFolderDestination.value
                    )
            }

            else -> {
                appData = null
                folderDestination = uriPathFolderDestination
            }
        }
        return DestinationAndAppDataForDownloadResult(
            folderDestination = folderDestination ?: run { throw NullFileException() },
            appData = appData,
        )
    }
}

