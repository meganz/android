package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set biometrics enabled use case
 *
 * @property passcodeRepository
 */
class EnableBiometricsUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val current = passcodeRepository.monitorPasscodeType().first()
            ?: throw IllegalStateException("Cannot enable biometrics without an existing passcode")
        if (current !is PasscodeType.Biometric) {
            passcodeRepository.setPasscodeType(PasscodeType.Biometric(current))
        }
    }
}