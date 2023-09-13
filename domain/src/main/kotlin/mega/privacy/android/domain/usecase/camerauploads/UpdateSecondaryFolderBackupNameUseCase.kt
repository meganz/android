package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import javax.inject.Inject

/**
 * Use Case that updates the Backup name of the Secondary Folder
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property isSecondaryFolderEnabled [IsSecondaryFolderEnabled]
 * @property updateBackupUseCase [UpdateBackupUseCase]
 */
class UpdateSecondaryFolderBackupNameUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val updateBackupUseCase: UpdateBackupUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupName The new Backup name
     */
    suspend operator fun invoke(backupName: String) {
        if (isSecondaryFolderEnabled() && backupName.isNotBlank()) {
            cameraUploadRepository.getMuBackUp()?.let { backup ->
                updateBackupUseCase(
                    backupId = backup.backupId,
                    backupName = backupName,
                    backupType = BackupInfoType.MEDIA_UPLOADS
                )
            }
        }
    }
}
