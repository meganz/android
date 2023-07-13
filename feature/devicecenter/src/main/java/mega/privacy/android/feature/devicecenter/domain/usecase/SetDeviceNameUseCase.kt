package mega.privacy.android.feature.devicecenter.domain.usecase

import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Use Case that sets a name to the current Device
 *
 * @property deviceCenterRepository [DeviceCenterRepository]
 */
class SetDeviceNameUseCase @Inject constructor(
    private val deviceCenterRepository: DeviceCenterRepository,
) {

    /**
     * Invocation function
     *
     * @param deviceName The Device Name
     */
    suspend operator fun invoke(deviceName: String) =
        deviceCenterRepository.setDeviceName(deviceName)
}