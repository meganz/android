package mega.privacy.android.feature.devicecenter.domain.usecase.folder

import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.backup.RemoveDeviceFolderConnectionUseCase
import javax.inject.Inject

/**
 * Implementation of [RemoveDeviceFolderConnectionUseCase] that removes a device folder connection
 */
internal class RemoveDeviceFolderConnectionUseCaseImpl @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) : RemoveDeviceFolderConnectionUseCase {

    override suspend operator fun invoke(backupId: Long): BackupRemovalStatus =
        cameraUploadsRepository.removeBackupFolder(backupId = backupId)
}
