package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupCameraUploadsBackupUseCase
import mega.privacy.android.domain.usecase.backup.SetupMediaUploadsBackupUseCase
import javax.inject.Inject

/**
 * UseCase that  Setup CameraUpload Setting and backup
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property setupMediaUploadsBackupUseCase [SetupCameraUploadsBackupUseCase]
 * @property removeBackupFolderUseCase [RemoveBackupFolderUseCase]
 */
class SetupMediaUploadSettingUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase,
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase,
) {

    /**
     * Invocation function
     *
     * @param isEnabled [Boolean]
     * @param mediaUploadName [String]
     */
    suspend operator fun invoke(isEnabled: Boolean, mediaUploadName: String) {
        cameraUploadRepository.setSecondaryEnabled(isEnabled)
        if (isEnabled) {
            setupMediaUploadsBackupUseCase(mediaUploadName)
        } else {
            removeBackupFolderUseCase(CameraUploadFolderType.Secondary)
        }
    }
}
