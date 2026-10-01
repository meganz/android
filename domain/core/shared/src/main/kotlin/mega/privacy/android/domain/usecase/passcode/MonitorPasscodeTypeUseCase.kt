package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Monitor passcode type use case
 *
 * @property passcodeRepository
 */
class MonitorPasscodeTypeUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke() = passcodeRepository.monitorPasscodeType()
}