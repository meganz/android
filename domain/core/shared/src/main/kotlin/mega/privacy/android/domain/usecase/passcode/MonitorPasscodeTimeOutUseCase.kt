package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Monitor passcode time out use case
 *
 * @property passcodeRepository
 */
class MonitorPasscodeTimeOutUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @return Flow of the passcode timeout
     */
    operator fun invoke(): Flow<PasscodeTimeout?> = passcodeRepository.monitorPasscodeTimeOut()
}