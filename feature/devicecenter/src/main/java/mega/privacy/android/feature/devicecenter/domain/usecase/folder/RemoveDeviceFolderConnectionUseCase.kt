package mega.privacy.android.feature.devicecenter.domain.usecase.folder

import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Remove device folder connection use case
 */
class RemoveDeviceFolderConnectionUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * invoke
     *
     * @param backupId specific backup id to remove
     */
    suspend operator fun invoke(backupId: Long): BackupRemovalStatus =
        cameraUploadsRepository.removeBackupFolder(backupId = backupId)

}
