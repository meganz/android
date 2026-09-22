package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Initialize both Remote and Local Backups
 */
class InitializeBackupsUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase,
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        setupOrUpdateCameraUploadsBackupUseCase(
            cameraUploadsRepository.getPrimarySyncHandle(),
            cameraUploadsRepository.getPrimaryFolderLocalPath()
        )
        setupOrUpdateMediaUploadsBackupUseCase(
            cameraUploadsRepository.getSecondarySyncHandle(),
            cameraUploadsRepository.getSecondaryFolderLocalPath()
        )
    }
}
