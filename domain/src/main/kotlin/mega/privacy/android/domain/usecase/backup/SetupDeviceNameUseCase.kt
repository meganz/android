package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.exception.ResourceAlreadyExistsMegaException
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Setup device name if not set
 */
class SetupDeviceNameUseCase @Inject constructor(
    private val getDeviceNameUseCase: GetDeviceNameUseCase,
    private val getDeviceIdUseCase: GetDeviceIdUseCase,
    private val setDeviceNameUseCase: RenameDeviceUseCase,
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invocation function
     */
    suspend operator fun invoke() {
        getDeviceIdUseCase()?.let { deviceId ->
            runCatching { getDeviceNameUseCase(deviceId) }.getOrNull().let {
                if (it.isNullOrEmpty()) {
                    var retry = false
                    var duplicateCounter = 0
                    do {
                        val deviceNameToSet = if (duplicateCounter == 0) {
                            environmentRepository.getDeviceInfo().device.trim()
                        } else {
                            "${environmentRepository.getDeviceInfo().device.trim()} ($duplicateCounter)"
                        }
                        runCatching {
                            setDeviceNameUseCase(
                                deviceId = deviceId,
                                deviceName = deviceNameToSet,
                            )
                        }.onSuccess {
                            retry = false
                        }.onFailure { exception ->
                            if (exception is ResourceAlreadyExistsMegaException) {
                                retry = true
                                duplicateCounter++
                            } else {
                                retry = false
                            }
                        }
                    } while (retry)
                }
            }
        }
    }
}
