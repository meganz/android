package mega.privacy.android.domain.usecase.transfers.offline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.pending.GetPendingTransfersByTypeUseCase
import javax.inject.Inject

/**
 * Use case to monitor whether the user started a download of a node for offline that is still
 * running.
 *
 * Re-checks [IsOfflineDownloadInProgressUseCase] whenever the pending or the active download
 * transfers change.
 */
class MonitorOfflineDownloadInProgressUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getPendingTransfersByTypeUseCase: GetPendingTransfersByTypeUseCase,
    private val isOfflineDownloadInProgressUseCase: IsOfflineDownloadInProgressUseCase,
) {
    /**
     * Invoke
     *
     * @param node the [TypedNode] to monitor
     * @return a [Flow] that emits whether the node has an offline download in progress.
     */
    operator fun invoke(node: TypedNode): Flow<Boolean> = combine(
        getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD),
        transferRepository.monitorActiveTransfersByType(TransferType.DOWNLOAD),
    ) { _, _ -> }
        .map { isOfflineDownloadInProgressUseCase(node) }
        .distinctUntilChanged()
}
