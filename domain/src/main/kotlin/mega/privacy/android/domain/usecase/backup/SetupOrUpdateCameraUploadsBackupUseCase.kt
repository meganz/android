package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateBackupUseCase
import javax.inject.Inject

/**
 * Setup Or Update CameraUploads Backup UseCase
 */
class SetupOrUpdateCameraUploadsBackupUseCase @Inject constructor(
    private val getCameraUploadBackupIDUseCase: GetCameraUploadBackupIDUseCase,
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase,
    private val updateBackupUseCase: UpdateBackupUseCase,
    private val cameraUploadRepository: CameraUploadRepository,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
) {

    /**
     * Invocation function
     * @param targetNode [Long]
     * @param localFolder [String]
     */
    suspend operator fun invoke(targetNode: Long?, localFolder: String?) {
        if (isCameraUploadsEnabledUseCase()) {
            getCameraUploadBackupIDUseCase()?.takeIf { it != -1L }?.let {
                updateBackupUseCase(
                    backupId = it,
                    backupName = cameraUploadRepository.getCameraUploadsName(),
                    backupType = BackupInfoType.CAMERA_UPLOADS,
                    targetNode = targetNode,
                    localFolder = localFolder
                )
            } ?: setupCameraUploadsBackupUseCase(cameraUploadRepository.getCameraUploadsName())
        }
    }
}
