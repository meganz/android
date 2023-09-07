package mega.privacy.android.feature.devicecenter.domain.usecase

import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Use Case that gets the device name from server
 *
 * @property deviceCenterRepository [DeviceCenterRepository]
 */
class GetDeviceNameUseCase @Inject constructor(
    private val deviceCenterRepository: DeviceCenterRepository,
) {
    /**
     * Invocation function
     *
     * @param deviceId The Device ID identifying the Device
     */
    suspend operator fun invoke(deviceId: String) =
        deviceCenterRepository.getDeviceName(deviceId = deviceId)
}
