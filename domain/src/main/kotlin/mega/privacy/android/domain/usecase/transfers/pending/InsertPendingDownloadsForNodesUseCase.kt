package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroupImpl
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetFileDestinationAndAppDataForDownloadUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to insert pending transfers for download a list of nodes to a specific destination.
 */
class InsertPendingDownloadsForNodesUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getPendingTransferNodeIdentifierUseCase: GetPendingTransferNodeIdentifierUseCase,
    private val doesUriPathHaveSufficientSpaceForNodesUseCase: DoesUriPathHaveSufficientSpaceForNodesUseCase,
    private val getFileDestinationAndAppDataForDownloadUseCase: GetFileDestinationAndAppDataForDownloadUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val timeSystemRepository: TimeSystemRepository,
) {
    /**
     * Invoke: insert pending transfers for download a list of nodes to a specific destination.
     * If the [destination] is not a valid folder a [NullFileException] is thrown.
     * If the actual destination ([destination] or cache if needed) has not enough space, a [NotEnoughStorageException] is thrown.
     * @param nodes the nodes to insert as download pending transfer
     * @param destination the destination to download the nodes. This is the final destination, cache folder can be used as the destination in pending transfers if needed.
     * @param isHighPriority whether this downloads are high priority (take precedence over current transfers) or not
     * @param appData app data to be added to this download (other app data will be added too, like group app data).
     */
    suspend operator fun invoke(
        nodes: List<TypedNode>,
        destination: UriPath,
        isHighPriority: Boolean,
        appData: TransferAppData?,
    ) {
        val (folderDestination, destinationAppData) = getFileDestinationAndAppDataForDownloadUseCase(
            destination
        )
        val transferGroupId = transferRepository.insertActiveTransferGroup(
            ActiveTransferGroupImpl(
                transferType = TransferType.DOWNLOAD,
                destination = destination.value,
                singleFileName = nodes.singleOrNull()?.name,
                startTime = timeSystemRepository.getCurrentTimeInMillis(),
            )
        )
        val appDataList = listOfNotNull(
            appData,
            destinationAppData,
            TransferAppData.TransferGroup(transferGroupId),
        )
        fileSystemRepository.createDirectory(folderDestination.value)
        if (!doesUriPathHaveSufficientSpaceForNodesUseCase(folderDestination, nodes)) {
            throw NotEnoughStorageException()
        }
        transferRepository.insertPendingTransfers(
            nodes.map { node ->
                InsertPendingTransferRequest(
                    transferType = TransferType.DOWNLOAD,
                    nodeIdentifier = getPendingTransferNodeIdentifierUseCase(node),
                    uriPath = UriPath(folderDestination.value.ensureEndsWithFileSeparator()),
                    appData = appDataList,
                    isHighPriority = isHighPriority,
                    fileName = node.name,
                )
            }
        )
    }

    private fun String.ensureEndsWithFileSeparator() =
        if (this.endsWith(File.separator)) {
            this
        } else {
            this.plus(File.separator)
        }
}