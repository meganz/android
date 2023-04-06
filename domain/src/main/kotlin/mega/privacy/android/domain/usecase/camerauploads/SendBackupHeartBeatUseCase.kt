package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Send Backup Heart Beat Use Case
 * @param cameraUploadRepository [CameraUploadRepository]
 */
class SendBackupHeartBeatUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {

    /**
     * Invocation function
     * @param backupId backup id identifying the backup
     * @param status   backup state
     * @param progress backup progress
     * @param ups      Number of pending upload transfers
     * @param downs    Number of pending download transfers
     * @param ts       Last action timestamp
     * @param lastNode Last node handle to be synced
     */
    suspend operator fun invoke(
        backupId: Long, status: Int, progress: Int, ups: Int, downs: Int,
        ts: Long, lastNode: Long,
    ) = cameraUploadRepository.sendBackupHeartbeat(
        backupId,
        status,
        progress,
        ups,
        downs,
        ts,
        lastNode,
    )
}
