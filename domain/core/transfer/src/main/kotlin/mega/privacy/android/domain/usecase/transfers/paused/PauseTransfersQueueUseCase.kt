package mega.privacy.android.domain.usecase.transfers.paused

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import javax.inject.Inject

/**
 * Pause Transfers Queue Use Case:
 * - If queue is paused: Nothing will be transferred, even if the individual state of the transfers is resumed.
 * - If queue is resumed: Only transfers with individual state resumed will be transferred.
 */
class PauseTransfersQueueUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(isPause: Boolean): Boolean {
        val isPauseResponse = transferRepository.pauseTransfers(isPause)
        val newBackupState: BackupState =
            if (isPauseResponse) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE
        updateCameraUploadsBackupStatesUseCase(newBackupState)
        return isPauseResponse
    }
}