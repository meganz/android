package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateMediaUploadsBackupUseCase
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupDataFromDatabaseUseCase
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupFolderUseCase
import javax.inject.Inject

/**
 * Remove backup folders logout task
 *
 * @property removeBackupFolderUseCase
 */
class RemoveBackupFoldersLogoutTask @Inject constructor(
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase,
    private val removeBackupDataFromDatabaseUseCase: RemoveBackupDataFromDatabaseUseCase,
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase,
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase,
    private val cameraUploadsRepository: CameraUploadsRepository,
) : LogoutTask {

    private var primaryBackupRemovalStatus: BackupRemovalStatus? = null
    private var secondaryBackupRemovalStatus: BackupRemovalStatus? = null

    /**
     * Remove backup folders
     *
     * removes primary and secondary backup folders
     * and stores the status of the removal
     */
    override suspend fun onPreLogout() {
        primaryBackupRemovalStatus = removeBackupFolderUseCase(CameraUploadFolderType.Primary)
        secondaryBackupRemovalStatus = removeBackupFolderUseCase(CameraUploadFolderType.Secondary)
    }

    /**
     * Remove backup data from database
     *
     * removes the backup data from the database based on the status of the removal
     */
    override suspend fun onLogoutSuccess() {
        primaryBackupRemovalStatus?.let {
            removeBackupDataFromDatabaseUseCase(it)
        }
        secondaryBackupRemovalStatus?.let {
            removeBackupDataFromDatabaseUseCase(it)
        }
    }

    /**
     * Initialize backups
     *
     * back ups are added again in case of logout failure
     */
    override suspend fun onLogoutFailed(throwable: Throwable) {
        if (primaryBackupRemovalStatus?.isOutdated == false) {
            setupOrUpdateCameraUploadsBackupUseCase(
                cameraUploadsRepository.getPrimarySyncHandle(),
                cameraUploadsRepository.getPrimaryFolderLocalPath()
            )
        }
        if (primaryBackupRemovalStatus?.isOutdated == false) {
            setupOrUpdateMediaUploadsBackupUseCase(
                cameraUploadsRepository.getSecondarySyncHandle(),
                cameraUploadsRepository.getSecondaryFolderLocalPath()
            )
        }
    }
}