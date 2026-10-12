package mega.privacy.android.domain.usecase.environment

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.environment.ThermalState
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Monitor thermal state of the device
 */
class MonitorDeviceThermalStateUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke(): Flow<ThermalState> = environmentRepository.monitorThermalState()
}
