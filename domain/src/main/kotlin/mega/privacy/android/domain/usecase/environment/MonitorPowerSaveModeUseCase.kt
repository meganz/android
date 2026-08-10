package mega.privacy.android.domain.usecase.environment

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use case for monitoring if the device is in power save mode (Battery Saver)
 */
class MonitorPowerSaveModeUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     *
     * @return flow that emits true when power save mode is enabled
     */
    operator fun invoke(): Flow<Boolean> = environmentRepository.monitorPowerSaveMode()
}
