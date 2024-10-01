package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.exception.NullFileException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetFileDestinationAndAppDataForDownloadUseCase
import mega.privacy.android.domain.usecase.transfers.mapper.GetPendingTransferNodeIdentifierUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to insert pending transfers for download a list of nodes to a specific destination.
 */
class InsertPendingDownloadsForNodesUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getPendingTransferNodeIdentifierUseCase: GetPendingTransferNodeIdentifierUseCase,
    private val doesPathHaveSufficientSpaceForNodesUseCase: DoesPathHaveSufficientSpaceForNodesUseCase,
    private val getFileDestinationAndAppDataForDownloadUseCase: GetFileDestinationAndAppDataForDownloadUseCase,
) {
    /**
     * Invoke: insert pending transfers for download a list of nodes to a specific destination.
     * If the [destination] is not a valid folder a [NullFileException] is thrown.
     * If the actual destination ([destination] or cache if needed) has not enough space, a [NotEnoughStorageException] is thrown.
     * @param nodes the nodes to insert as download pending transfer
     * @param destination the destination to download the nodes. This is the final destination, cache folder can be used as the destination in pending transfers if needed.
     * @param isHighPriority whether this downloads are high priority (take precedence over current transfers) or not
     */
    suspend operator fun invoke(
        nodes: List<TypedNode>,
        destination: UriPath,
        isHighPriority: Boolean,
    ) {
        val (folderDestination, appData) = getFileDestinationAndAppDataForDownloadUseCase(destination)
        if (!doesPathHaveSufficientSpaceForNodesUseCase(folderDestination.value, nodes)) {
            throw NotEnoughStorageException()
        }
        transferRepository.insertPendingTransfers(
            nodes.map { node ->
                InsertPendingTransferRequest(
                    transferType = TransferType.DOWNLOAD,
                    nodeIdentifier = getPendingTransferNodeIdentifierUseCase(node),
                    path = folderDestination.value.ensureEndsWithFileSeparator(),
                    appData = appData,
                    isHighPriority = isHighPriority,
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