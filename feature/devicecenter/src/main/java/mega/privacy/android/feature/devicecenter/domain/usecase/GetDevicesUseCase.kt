package mega.privacy.android.feature.devicecenter.domain.usecase

import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Use Case that retrieves all of the User's Backup Devices
 *
 * @property deviceCenterRepository [DeviceCenterRepository]
 */
class GetDevicesUseCase @Inject constructor(
    private val deviceCenterRepository: DeviceCenterRepository,
) {
    /**
     * Invocation function
     *
     * @return The User's Backup Devices
     */
    suspend operator fun invoke() = with(deviceCenterRepository) {
        getDevices(
            currentDeviceId = getDeviceId().orEmpty(),
            backupInfoList = getBackupInfo(),
            deviceIdAndNameMap = getDeviceIdAndNameMap(),
        )
    }
}