package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.getSDCardTransferPath
import mega.privacy.android.domain.entity.transfer.isSDCardDownload
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import java.io.File
import javax.inject.Inject

/**
 * Handles transfer events in case the event is related to Sd card transfer:
 * - When a transfer to the Sd card starts it inserts the related entity to the database
 * - When a file transfer to the Sd card finishes it moves the file from cache to the final destination
 * - When a root transfer finishes it deletes the related entity from the database
 */
class HandleSDCardEventUseCase @Inject constructor(
    private val insertSdTransferUseCase: InsertSdTransferUseCase,
    private val deleteSdTransferByTagUseCase: DeleteSdTransferByTagUseCase,
    private val getOrCreateStorageDownloadLocationUseCase: GetOrCreateStorageDownloadLocationUseCase,
    private val moveFileToSdCardUseCase: MoveFileToSdCardUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val transferRepository: TransferRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(transferEvent: TransferEvent) {
        val transfer = transferEvent.transfer
        when (transferEvent) {
            is TransferEvent.TransferStartEvent -> {
                if (transfer.isSDCardDownload()) {
                    insertSdTransferUseCase(
                        SdTransfer(
                            transfer.tag,
                            transfer.fileName,
                            transfer.totalBytes.toString(),
                            transfer.nodeHandle.toString(),
                            transfer.localPath,
                            transfer.appData
                        )
                    )
                }
            }

            is TransferEvent.TransferFinishEvent -> {
                if (transferEvent.error != null) return
                scope.launch {
                    if (!transfer.isFolderTransfer
                        && fileSystemRepository.isSDCardCachePath(transfer.localPath)
                    ) {
                        val movePath =
                            transfer.getSDCardTransferPath()?.takeIf { transfer.isRootTransfer }
                                ?: getChildFinalDestination(transfer.localPath)

                        movePath?.let { path ->
                            moveFileToSdCardUseCase(
                                File(transferEvent.transfer.localPath),
                                path
                            )
                        }
                    }
                    if (transfer.isRootTransfer && transfer.isSDCardDownload()) {
                        //if it's a root sd card transfer we can remove the sd transfer from the data base
                        deleteSdTransferByTagUseCase(transferEvent.transfer.tag)
                    }
                }
            }

            else -> {} //nothing here
        }
    }

    /**
     * Child transfers doesn't have app data, so we can't get [TransferAppData.SdCardDownload]
     * To know the destination on the sd card we replace the cache folder by the destination folder
     */
    private suspend fun getChildFinalDestination(currentFilePath: String): String? {
        val file = File(currentFilePath)
        val currentFolderPath = file.parent
        val cacheRoot = transferRepository.getOrCreateSDCardTransfersCacheFolder()?.path
        val destinationRoot = getOrCreateStorageDownloadLocationUseCase()
        return if (cacheRoot != null
            && destinationRoot != null
            && currentFolderPath.startsWith(cacheRoot)
        ) {
            destinationRoot.plus(currentFolderPath.removePrefix(cacheRoot))
        } else {
            null
        }
    }
}