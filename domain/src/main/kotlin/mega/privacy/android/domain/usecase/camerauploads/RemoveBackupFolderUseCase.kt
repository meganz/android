package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Remove backup folder use case
 *
 * When user tries to logout, should delete backups first.
 * This should be called before Logout UseCase(megaApi.logout()).
 */
class RemoveBackupFolderUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {

    /**
     * invoke
     *
     * @param cameraUploadFolderType selects primary or secondary folder type
     */
    suspend operator fun invoke(cameraUploadFolderType: CameraUploadFolderType) {
        cameraUploadRepository.getBackupFolderId(cameraUploadFolderType)?.let {
            val (backupId, code) = cameraUploadRepository.removeBackupFolder(backupId = it)
            if (code == OK_CODE) {
                cameraUploadRepository.deleteBackupById(backupId = backupId)
            } else {
                cameraUploadRepository.setBackupAsOutdated(backupId = backupId)
            }
        }
    }

    companion object {
        private const val OK_CODE = 0
    }
}