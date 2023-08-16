package mega.privacy.android.feature.devicecenter.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Use Case that retrieves all of the User's Backup Devices
 *
 * @property deviceCenterRepository [DeviceCenterRepository]
 * @property isCameraUploadsEnabledUseCase [IsCameraUploadsEnabledUseCase]
 */
class GetDevicesUseCase @Inject constructor(
    private val deviceCenterRepository: DeviceCenterRepository,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
) {
    /**
     * Invocation function
     *
     * @return The User's Backup Devices
     */
    suspend operator fun invoke() = deviceCenterRepository.getDevices(
        currentDeviceId = deviceCenterRepository.getDeviceId().orEmpty(),
        backupInfoList = deviceCenterRepository.getBackupInfo(),
        deviceIdAndNameMap = deviceCenterRepository.getDeviceIdAndNameMap(),
        isCameraUploadsEnabled = isCameraUploadsEnabledUseCase(),
    )
}