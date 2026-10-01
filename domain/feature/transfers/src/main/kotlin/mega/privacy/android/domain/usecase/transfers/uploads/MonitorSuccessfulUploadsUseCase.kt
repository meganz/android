package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import javax.inject.Inject

/**
 * Monitor uploads started by the user that finish without error, whether they are files or folders.
 *
 * Camera and chat uploads are excluded because they are not the result of an explicit upload
 * action.
 *
 * @property monitorTransferEventsUseCase [MonitorTransferEventsUseCase]
 */
class MonitorSuccessfulUploadsUseCase @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
) {
    /**
     * Invoke
     *
     * @return a [Flow] emitting each successfully uploaded [Transfer]
     */
    operator fun invoke(): Flow<Transfer> = monitorTransferEventsUseCase()
        .filterIsInstance<TransferEvent.TransferFinishEvent>()
        .filter { event ->
            event.error == null && event.transfer.transferType == TransferType.GENERAL_UPLOAD
        }
        .map { it.transfer }
}
