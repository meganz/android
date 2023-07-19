package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupFolderUseCase
import javax.inject.Inject

/**
 * LogoutUseCase use case
 */
class LogoutUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() {
        loginRepository.setLogoutInProgressFlag(true)
        runCatching {
            removeBackupFolderUseCase(CameraUploadFolderType.Primary)
            removeBackupFolderUseCase(CameraUploadFolderType.Secondary)
            loginRepository.logout()
        }.onFailure {
            loginRepository.setLogoutInProgressFlag(false)
            throw it
        }
    }
}
