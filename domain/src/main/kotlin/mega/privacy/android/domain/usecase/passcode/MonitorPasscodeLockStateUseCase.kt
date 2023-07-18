package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject


/**
 * Monitor passcode lock state use case
 *
 * @property passcodeRepository
 */
class MonitorPasscodeLockStateUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {

    /**
     * Invoke
     *
     * @return flow of locked state
     */
    operator fun invoke() = passcodeRepository.monitorLockState()
}
