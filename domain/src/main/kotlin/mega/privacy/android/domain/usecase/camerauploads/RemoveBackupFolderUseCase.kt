package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Remove backup folder use case
 *
 * When user tries to logout, should delete backups first.
 * This should be called before Logout UseCase(megaApi.logout()).
 */
class RemoveBackupFolderUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {

    /**
     * invoke
     *
     * @param cameraUploadFolderType selects primary or secondary folder type
     */
    suspend operator fun invoke(cameraUploadFolderType: CameraUploadFolderType) {
        cameraUploadsRepository.getBackupFolderId(cameraUploadFolderType)?.let {
            val (backupId, code) = cameraUploadsRepository.removeBackupFolder(backupId = it)
            if (code == OK_CODE) {
                cameraUploadsRepository.deleteBackupById(backupId = backupId)
            } else {
                cameraUploadsRepository.setBackupAsOutdated(backupId = backupId)
            }
        }
    }

    companion object {
        private const val OK_CODE = 0
    }
}