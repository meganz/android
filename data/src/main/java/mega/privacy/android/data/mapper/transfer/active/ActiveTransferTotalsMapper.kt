package mega.privacy.android.data.mapper.transfer.active

import dagger.Lazy
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferAppData
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
     * @param previousActionGroups The previously returned groups, this will be used to optimize performance, as groups can't be changed.
     */
    suspend operator fun invoke(
        type: TransferType,
        list: List<ActiveTransfer>,
        transferredBytes: Map<Long, Long>,
        previousActionGroups: List<ActiveTransferTotals.ActionGroup>? = null,
    ): ActiveTransferTotals {
        val onlyFiles = list.filter { !it.isFolderTransfer }
        val actionGroups = onlyFiles
            .groupBy { it.getTransferGroup()?.groupId }
            .mapNotNull { (key, activeTransfersFiles) ->
                key?.toInt()?.let { groupId ->
                    val previousGroup = previousActionGroups?.firstOrNull { it.groupId == groupId }
                    val transferGroup = if (previousGroup == null) {
                        transferRepository.get().getActiveTransferGroupById(groupId)
                    } else null
                    val destination = previousGroup?.destination ?: transferGroup?.destination
                    val startTime = previousGroup?.startTime ?: transferGroup?.startTime ?: 0
                    val pendingTransferNodeIdentifier =
                        previousGroup?.pendingTransferNodeId ?: transferGroup?.pendingTransferNodeId
                    val names = previousGroup?.selectedNames
                        ?: transferGroup?.selectedNames
                        ?: activeTransfersFiles.map { it.fileName }
                    destination?.let { dest ->
                        ActiveTransferTotals.ActionGroup(
                            groupId = groupId,
                            totalFiles = activeTransfersFiles.size,
                            finishedFiles = activeTransfersFiles.count { it.isFinished },
                            completedFiles = activeTransfersFiles.count { it.isFinished && transferredBytes[it.uniqueId] == it.totalBytes },
                            alreadyTransferred = activeTransfersFiles.count { it.isAlreadyTransferred },
                            destination = dest,
                            selectedNames = names,
                            singleTransferTag = activeTransfersFiles.singleOrNull()?.tag,
                            startTime = startTime,
                            pausedFiles = activeTransfersFiles.count { it.isPaused },
                            totalBytes = activeTransfersFiles.sumOf { it.totalBytes },
                            transferredBytes = activeTransfersFiles.sumOf {
                                //if it's finished always totalBytes as it can be cancelled or failed
                                if (it.isFinished) it.totalBytes else transferredBytes[it.uniqueId]
                                    ?: 0L
                            },
                            pendingTransferNodeId = pendingTransferNodeIdentifier,
                            appData = activeTransfersFiles
                                .flatMap { it.appData }
                                .filterNot { it is TransferAppData.TransferGroup } //group would be redundant
                                .distinctBy { it::class } //only one of each type representing the group
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
            totalCompletedFileTransfers = onlyFiles.count { it.isFinished && transferredBytes[it.uniqueId] == it.totalBytes },
            totalBytes = onlyFiles.sumOf { it.totalBytes },
            transferredBytes = onlyFiles.sumOf {
                //if it's finished always totalBytes as it can be cancelled or failed
                if (it.isFinished) it.totalBytes else transferredBytes[it.uniqueId] ?: 0L
            },
            totalAlreadyTransferredFiles = onlyFiles.count { it.isAlreadyTransferred },
            totalCancelled = list.count { it.isCancelled },
            actionGroups = actionGroups
        )
    }
}