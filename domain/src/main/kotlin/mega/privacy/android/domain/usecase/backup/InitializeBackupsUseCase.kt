package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetMediaUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdatePrimaryFolderBackupNameUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateSecondaryFolderBackupNameUseCase
import javax.inject.Inject

/**
 * Initialize both Remote and Local Backups
 */
class InitializeBackupsUseCase @Inject constructor(
    private val setupDeviceNameUseCase: SetupDeviceNameUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val getCameraUploadBackupIDUseCase: GetCameraUploadBackupIDUseCase,
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase,
    private val updatePrimaryFolderBackupNameUseCase: UpdatePrimaryFolderBackupNameUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val getMediaUploadBackupIDUseCase: GetMediaUploadBackupIDUseCase,
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase,
    private val updateSecondaryFolderBackupNameUseCase: UpdateSecondaryFolderBackupNameUseCase,
    private val cameraUploadsRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        setupDeviceNameUseCase()
        val cameraUploadsName = cameraUploadsRepository.getCameraUploadsName()
        val cuBackupID = getCameraUploadBackupIDUseCase()
        if (isCameraUploadsEnabledUseCase() && cuBackupID == null) {
            setupCameraUploadsBackupUseCase(cameraUploadsName)
        } else if (cuBackupID != null) {
            updatePrimaryFolderBackupNameUseCase(cameraUploadsName)
        }
        val muBackupID = getMediaUploadBackupIDUseCase()
        val mediaUploadsName = cameraUploadsRepository.getMediaUploadsName()
        if (isSecondaryFolderEnabled() && muBackupID == null) {
            setupMediaUploadsBackupUseCase(mediaUploadsName)
        } else if (muBackupID != null) {
            updateSecondaryFolderBackupNameUseCase(mediaUploadsName)
        }
    }
}
