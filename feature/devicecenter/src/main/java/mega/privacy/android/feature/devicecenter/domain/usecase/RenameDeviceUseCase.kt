package mega.privacy.android.feature.devicecenter.domain.usecase

import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Use Case that renames the User's selected Device
 *
 * @property deviceCenterRepository [DeviceCenterRepository]
 */
class RenameDeviceUseCase @Inject constructor(
    private val deviceCenterRepository: DeviceCenterRepository,
) {
    /**
     * Invocation function
     *
     * @param deviceId The Device ID identifying the Device to be renamed
     * @param deviceName The new Device Name
     */
    suspend operator fun invoke(deviceId: String, deviceName: String) =
        deviceCenterRepository.renameDevice(
            deviceId = deviceId,
            deviceName = deviceName,
        )
}