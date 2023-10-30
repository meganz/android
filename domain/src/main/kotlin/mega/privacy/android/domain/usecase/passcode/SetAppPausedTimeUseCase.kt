package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set app paused time use case
 *
 * @property monitorPasscodeLockStateUseCase
 * @property passcodeRepository
 */
class SetAppPausedTimeUseCase @Inject constructor(
    private val monitorPasscodeLockStateUseCase: MonitorPasscodeLockStateUseCase,
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @param currentTime
     */
    suspend operator fun invoke(
        currentTime: Long,
        orientation: Int,
    ) {
        val locked = monitorPasscodeLockStateUseCase().firstOrNull() ?: false
        if (!locked) {
            passcodeRepository.setLastPausedTime(currentTime)
            passcodeRepository.setLastOrientation(orientation)
        }
    }
}
