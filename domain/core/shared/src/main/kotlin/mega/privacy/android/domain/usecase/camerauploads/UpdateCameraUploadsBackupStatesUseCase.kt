package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import javax.inject.Inject

/**
 * Use Case to update the [BackupState] of both Primary and Secondary Folders of Camera Uploads
 *
 * @property updatePrimaryFolderBackupStateUseCase [UpdatePrimaryFolderBackupStateUseCase]
 * @property updateSecondaryFolderBackupStateUseCase [UpdateSecondaryFolderBackupStateUseCase]
 */
class UpdateCameraUploadsBackupStatesUseCase @Inject constructor(
    private val updatePrimaryFolderBackupStateUseCase: UpdatePrimaryFolderBackupStateUseCase,
    private val updateSecondaryFolderBackupStateUseCase: UpdateSecondaryFolderBackupStateUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupState The new [BackupState] of both Folders
     */
    suspend operator fun invoke(backupState: BackupState) {
        updatePrimaryFolderBackupStateUseCase.invoke(backupState)
        updateSecondaryFolderBackupStateUseCase.invoke(backupState)
    }
}
