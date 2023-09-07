package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that gets the device name from server
 *
 * @property backupRepository [BackupRepository]
 */
class GetDeviceNameUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    /**
     * Invocation function
     *
     * @param deviceId The Device ID identifying the Device
     */
    suspend operator fun invoke(deviceId: String) =
        backupRepository.getDeviceName(deviceId = deviceId)
}
