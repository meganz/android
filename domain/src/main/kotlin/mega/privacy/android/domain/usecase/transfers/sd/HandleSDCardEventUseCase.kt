package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.transfer.DestinationUriAndSubFolders
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isSDCardDownload
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Handles transfer events in case the event is related to Sd card transfer:
 * - When a transfer to the Sd card receives an event different than finish, it updates the parent path in the database
 * - When a file transfer to the Sd card finishes it moves the file from cache to the final destination
 * - When a root transfer finishes it deletes the related entity from the database
 */
class HandleSDCardEventUseCase @Inject constructor(
    private val moveFileToSdCardUseCase: MoveFileToSdCardUseCase,
    private val fileSystemRepository: FileSystemRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(
        transferEvent: TransferEvent,
        destinationUriAndSubFolders: DestinationUriAndSubFolders?,
    ) {
        val transfer = transferEvent.transfer
        val path = transfer.localPath.takeIf { it.isNotEmpty() } ?: transfer.parentPath
        if (transfer.transferType != TransferType.DOWNLOAD
            || !(transfer.isSDCardDownload() || fileSystemRepository.isSDCardCachePath(path))
        ) return

        if (transferEvent is TransferEvent.TransferFinishEvent
            && transferEvent.error == null
            && transfer.isFolderTransfer.not()
        ) {
            scope.launch {
                destinationUriAndSubFolders?.let { (path, subFolders) ->
                    moveFileToSdCardUseCase(
                        File(transfer.localPath),
                        path,
                        subFolders
                    )
                }
            }
        }
    }
}