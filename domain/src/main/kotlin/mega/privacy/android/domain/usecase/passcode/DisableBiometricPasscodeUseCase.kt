package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Disable biometric passcode use case
 *
 * @property passcodeRepository
 */
class DisableBiometricPasscodeUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val current = getCurrentBiometricPasscodeType()
        passcodeRepository.setPasscodeType(current.fallback)
    }

    private suspend fun getCurrentBiometricPasscodeType() =
        (passcodeRepository.monitorPasscodeType()
            .firstOrNull() as? PasscodeType.Biometric
            ?: throw IllegalStateException("Cannot disable biometric if current passcode type is not biometric"))
}