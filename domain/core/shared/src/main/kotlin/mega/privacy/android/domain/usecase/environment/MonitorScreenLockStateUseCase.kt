package mega.privacy.android.domain.usecase.environment

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.environment.ScreenLockState
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use case for monitoring the device screen lock state.
 */
class MonitorScreenLockStateUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     *
     * @return a Flow that emits a [ScreenLockState] whenever the screen is locked or unlocked.
     */
    operator fun invoke(): Flow<ScreenLockState> = environmentRepository.monitorScreenLockState()
}
