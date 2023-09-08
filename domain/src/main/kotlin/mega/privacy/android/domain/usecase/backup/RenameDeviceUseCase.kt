package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that renames the User's selected Device
 *
 * @property backupRepository [BackupRepository]
 */
class RenameDeviceUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    /**
     * Invocation function
     *
     * @param deviceId The Device ID identifying the Device to be renamed
     * @param deviceName The new Device Name
     */
    suspend operator fun invoke(deviceId: String, deviceName: String) =
        backupRepository.renameDevice(
            deviceId = deviceId,
            deviceName = deviceName,
        )
}