package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use case to get current [BatteryInfo]
 */
class GetBatteryInfoUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke(): BatteryInfo = environmentRepository.getBatteryInfo()
}
