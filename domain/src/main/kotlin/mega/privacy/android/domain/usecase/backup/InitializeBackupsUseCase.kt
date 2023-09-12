package mega.privacy.android.domain.usecase.backup

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
) {

    /**
     * Invocation function
     * @param cameraUploadName
     * @param mediaUploadName
     */
    suspend operator fun invoke(cameraUploadName: String, mediaUploadName: String) {
        setupDeviceNameUseCase()
        val cuBackupID = getCameraUploadBackupIDUseCase()
        if (isCameraUploadsEnabledUseCase() && cuBackupID == null) {
            setupCameraUploadsBackupUseCase(cameraUploadName)
        } else if (cuBackupID != null) {
            updatePrimaryFolderBackupNameUseCase(cameraUploadName)
        }
        val muBackupID = getMediaUploadBackupIDUseCase()
        if (isSecondaryFolderEnabled() && muBackupID == null) {
            setupMediaUploadsBackupUseCase(mediaUploadName)
        } else if (muBackupID != null) {
            updateSecondaryFolderBackupNameUseCase(mediaUploadName)
        }
    }
}
