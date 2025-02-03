package mega.privacy.android.data.mapper.transfer.active

import dagger.Lazy
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.getTransferGroup
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Mapper for converting a list of [ActiveTransferEntity] into [ActiveTransferTotals].
 */
internal class ActiveTransferTotalsMapper @Inject constructor(
    private val transferRepository: Lazy<TransferRepository>,
) {
    /**
     * @param type
     * @param list the list of active transfers of which the total will be calculated
     * @param transferredBytes Map of transfer tag to transferred bytes
     * @param previousGroups The previously returned groups, this will be used to optimize performance, as groups can be changed.
     */
    suspend operator fun invoke(
        type: TransferType,
        list: List<ActiveTransfer>,
        transferredBytes: Map<Int, Long>,
        previousGroups: List<ActiveTransferTotals.Group>? = null,
    ): ActiveTransferTotals {
        val onlyFiles = list.filter { !it.isFolderTransfer }
        val groups = onlyFiles
            .groupBy { it.getTransferGroup()?.groupId }
            .mapNotNull { (key, activeTransfersFiles) ->
                key?.toInt()?.let { groupId ->
                    val previousGroup = previousGroups?.firstOrNull { it.groupId == groupId }
                    val transferGroup = if (previousGroup == null) {
                        transferRepository.get().getActiveTransferGroupById(groupId)
                    } else null
                    val destination = previousGroup?.destination ?: transferGroup?.destination
                    val fileName = previousGroup?.singleFileName ?: transferGroup?.singleFileName
                    destination?.let { destination ->
                        ActiveTransferTotals.Group(
                            groupId = groupId,
                            totalFiles = activeTransfersFiles.size,
                            finishedFiles = activeTransfersFiles.count { it.isFinished },
                            completedFiles = activeTransfersFiles.count { it.isFinished && transferredBytes[it.tag] == it.totalBytes },
                            alreadyTransferred = activeTransfersFiles.count { it.isAlreadyTransferred },
                            destination = destination,
                            singleFileName = fileName,
                        )
                    }
                }
            }
        return ActiveTransferTotals(
            transfersType = type,
            totalTransfers = list.size,
            totalFileTransfers = onlyFiles.size,
            pausedFileTransfers = onlyFiles.count { it.isPaused },
            totalFinishedTransfers = list.count { it.isFinished },
            totalFinishedFileTransfers = onlyFiles.count { it.isFinished },
            totalCompletedFileTransfers = onlyFiles.count { it.isFinished && transferredBytes[it.tag] == it.totalBytes },
            totalBytes = onlyFiles.sumOf { it.totalBytes },
            transferredBytes = onlyFiles.sumOf {
                //if it's finished always totalBytes as it can be cancelled or failed
                if (it.isFinished) it.totalBytes else transferredBytes[it.tag] ?: 0L
            },
            totalAlreadyTransferredFiles = onlyFiles.count { it.isAlreadyTransferred },
            totalCancelled = list.count { it.isCancelled },
            groups = groups
        )
    }
}