package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
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
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
) {

    /**
     * Invocation function
     * @param targetNode [Long]
     * @param localFolder [String]
     */
    suspend operator fun invoke(targetNode: Long?, localFolder: String?) {
        if (isMediaUploadsEnabledUseCase()) {
            getMediaUploadBackupIDUseCase()?.takeIf { it != -1L }?.let {
                updateBackupUseCase(
                    backupId = it,
                    backupName = cameraUploadsRepository.getMediaUploadsName(),
                    backupType = BackupInfoType.MEDIA_UPLOADS,
                    targetNode = targetNode,
                    localFolder = localFolder
                )
            } ?: setupMediaUploadsBackupUseCase(cameraUploadsRepository.getMediaUploadsName())
        }
    }
}
