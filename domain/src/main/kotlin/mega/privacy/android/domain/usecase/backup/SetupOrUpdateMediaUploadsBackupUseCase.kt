package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetMediaUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateBackupUseCase
import javax.inject.Inject

/**
 * Setup Or Update Media Uploads Backup UseCase
 */
class SetupOrUpdateMediaUploadsBackupUseCase @Inject constructor(
    private val getMediaUploadBackupIDUseCase: GetMediaUploadBackupIDUseCase,
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase,
    private val updateBackupUseCase: UpdateBackupUseCase,
    private val cameraUploadRepository: CameraUploadRepository,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
) {

    /**
     * Invocation function
     * @param targetNode [Long]
     * @param localFolder [String]
     */
    suspend operator fun invoke(targetNode: Long?, localFolder: String?) {
        if (isSecondaryFolderEnabled()) {
            getMediaUploadBackupIDUseCase()?.takeIf { it != -1L }?.let {
                updateBackupUseCase(
                    backupId = it,
                    backupName = cameraUploadRepository.getMediaUploadsName(),
                    backupType = BackupInfoType.MEDIA_UPLOADS,
                    targetNode = targetNode,
                    localFolder = localFolder
                )
            } ?: setupMediaUploadsBackupUseCase(cameraUploadRepository.getMediaUploadsName())
        }
    }
}
