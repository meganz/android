package mega.privacy.android.domain.usecase.transfers.sd

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferIfNotExistUseCase
import java.io.File
import javax.inject.Inject

/**
 * Handles SD card transfers that are not in progress anymore:
 * - If the transfer is not in progress anymore, it adds it to the completed transfers
 *   if the SDK still has its reference
 * - If the SDK download:
 *    - Does not exist, nothing can be done.
 *    - Does exist, checks the final file, which is a copy of the SDK download:
 *        - If the sizes are the same, it means the copy was successfully done
 *            and the SDK download is deleted.
 *        - If the sizes are different, it means the copy was not successful,
 *            the final file is deleted and the copy starts from the scratch.
 */
class HandleNotInProgressSDCardActiveTransfersUseCase @Inject constructor(
    private val addCompletedTransferIfNotExistUseCase: AddCompletedTransferIfNotExistUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val moveFileToSdCardUseCase: MoveFileToSdCardUseCase,
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(
        notInProgressSdCardAppDataMap: Map<Int, List<TransferAppData>>,
    ) {
        buildList {
            notInProgressSdCardAppDataMap.keys.forEach { tag ->
                getTransferByTagUseCase(tag)?.let { add(it) }
            }
        }.also { addCompletedTransferIfNotExistUseCase(it) }

        notInProgressSdCardAppDataMap.values.forEach { appData ->
            (appData.find { it is TransferAppData.SdCardDownload } as TransferAppData.SdCardDownload)
                .let { sdCardAppData ->
                    if (sdCardAppData.parentPath.isNullOrEmpty().not()) {
                        scope.launch {
                            fileSystemRepository.getFileByPath(sdCardAppData.targetPathForSDK)
                                ?.let { sdkDownloadedFile ->
                                    if (sdkDownloadedFile.exists()) {
                                        val finalFileLength =
                                            fileSystemRepository.getFileLengthFromSdCardContentUri(
                                                sdCardAppData.finalTargetUri
                                            )

                                        if (finalFileLength == sdkDownloadedFile.length()) {
                                            fileSystemRepository.deleteFile(sdkDownloadedFile)
                                        } else {
                                            fileSystemRepository.deleteFileFromSdCardContentUri(
                                                sdCardAppData.finalTargetUri
                                            )

                                            val subFolders = sdCardAppData.parentPath
                                                ?.removePrefix(sdCardAppData.targetPathForSDK)
                                                ?.split(File.separator)
                                                ?.filter { it.isNotBlank() } ?: emptyList()

                                            moveFileToSdCardUseCase(
                                                sdkDownloadedFile,
                                                sdCardAppData.finalTargetUri,
                                                subFolders
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }
        }

        transferRepository.removeInProgressTransfers(notInProgressSdCardAppDataMap.keys.toSet())
    }
}
