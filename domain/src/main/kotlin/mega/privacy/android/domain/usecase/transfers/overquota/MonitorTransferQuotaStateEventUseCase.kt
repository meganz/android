package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.TransferQuotaStateEvent
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor the transfer quota state the SDK predicts, from both paths it can arrive by.
 *
 * A streaming read has no transfer to report on, so the SDK raises a global event for it, on the
 * folder link instance when the media comes from a folder link. A batch download reports it to the
 * listener of the batch that asked, which is broadcast on from there.
 */
class MonitorTransferQuotaStateEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @return a [Flow] emitting the predicted [TransferQuotaState] each time one is reported.
     */
    operator fun invoke(): Flow<TransferQuotaState> = merge(
        globalEvents()
            .filterIsInstance<TransferQuotaStateEvent>()
            .map { it.state },
        transferRepository.monitorTransferQuotaState(),
    )

    private fun globalEvents() = merge(
        notificationsRepository.monitorEvent(),
        notificationsRepository.monitorFolderLinkEvent(),
    )
}
