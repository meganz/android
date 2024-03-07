package mega.privacy.android.domain.usecase.environment

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Monitor charging stopped State
 */
class MonitorBatteryInfoUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke(): Flow<BatteryInfo> = environmentRepository.monitorBatteryInfo()
}
