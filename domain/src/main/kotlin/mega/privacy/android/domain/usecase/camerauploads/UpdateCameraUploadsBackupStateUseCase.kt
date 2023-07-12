package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import javax.inject.Inject

/**
 * Use case for updating backup state for camera uploads
 */
class UpdateCameraUploadsBackupStateUseCase @Inject constructor(
    private val updateCameraUploadsStateUseCase: UpdateCameraUploadsBackupUseCase,
    private val updateMediaUploadsBackupUseCase: UpdateMediaUploadsBackupUseCase,
) {

    /**
     * @param primaryFolderName     primary backup folder name
     * @param secondaryFolderName   secondary backup folder name
     * @param backupState       Backup state
     */
    suspend operator fun invoke(
        backupState: BackupState,
        primaryFolderName: String,
        secondaryFolderName: String,
    ) {
        updateCameraUploadsStateUseCase.invoke(
            backupName = primaryFolderName,
            backupState = backupState
        )

        updateMediaUploadsBackupUseCase.invoke(
            backupName = secondaryFolderName,
            backupState = backupState
        )
    }
}
