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
    suspend operator fun invoke(
        type: TransferType,
        list: List<ActiveTransfer>,
        transferredBytes: Map<Int, Long>,
    ): ActiveTransferTotals {
        val onlyFiles = list.filter { !it.isFolderTransfer }
        val groups = onlyFiles
            .groupBy { it.getTransferGroup()?.groupId }
            .mapNotNull { (key, activeTransfers) ->
                key?.toInt()?.let { groupId ->
                    transferRepository.get()
                        .getActiveTransferGroupById(groupId)?.destination?.let { destination ->
                        ActiveTransferTotals.Group(
                            groupId = groupId,
                            totalFiles = activeTransfers.size,
                            finishedFiles = activeTransfers.count { it.isFinished },
                            destination = destination,
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