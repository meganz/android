package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

class RemoveBackupDataFromDatabaseUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository
) {

    suspend operator fun invoke(backupRemovalStatus: BackupRemovalStatus) {
        if (backupRemovalStatus.isOutdated) {
            cameraUploadsRepository.setBackupAsOutdated(backupId = backupRemovalStatus.backupId)
        } else {
            cameraUploadsRepository.deleteBackupById(backupId = backupRemovalStatus.backupId)
        }
    }
}