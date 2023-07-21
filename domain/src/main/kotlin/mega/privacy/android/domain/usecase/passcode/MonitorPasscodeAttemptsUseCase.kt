package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Monitor passcode attempts use case
 */
class MonitorPasscodeAttemptsUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @return flow of failed attempts count
     */
    operator fun invoke() = passcodeRepository.monitorFailedAttempts().map { it ?: 0 }
}
