package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupFolderUseCase
import javax.inject.Inject

/**
 * Remove backup folders logout task
 *
 * @property removeBackupFolderUseCase
 */
class RemoveBackupFoldersLogoutTask @Inject constructor(
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase,
) : LogoutTask {
    /**
     * Invoke
     *
     */
    override suspend fun invoke() {
        removeBackupFolderUseCase(CameraUploadFolderType.Primary)
        removeBackupFolderUseCase(CameraUploadFolderType.Secondary)
    }
}