package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Monitor passcode lock preference use case
 *
 * @property passcodeRepository
 */
class MonitorPasscodeLockPreferenceUseCase @Inject constructor(private val passcodeRepository: PasscodeRepository) {
    /**
     * Invoke
     *
     * @return passcode lock enabled state or false if not set
     */
    operator fun invoke() =
        passcodeRepository.monitorIsPasscodeEnabled()
            .map { it ?: false }
}