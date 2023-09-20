package mega.privacy.android.domain.usecase.backup

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
                    setDeviceNameUseCase(deviceId, environmentRepository.getDeviceInfo().device)
                }
            }
        }
    }
}
