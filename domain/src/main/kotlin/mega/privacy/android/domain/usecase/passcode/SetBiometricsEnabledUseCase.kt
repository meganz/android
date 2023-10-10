package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Set biometrics enabled use case
 *
 * @property passcodeRepository
 */
@OptIn(ExperimentalContracts::class)
class SetBiometricsEnabledUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
) {
    /**
     * Invoke
     *
     * @param enabled
     */
    suspend operator fun invoke(enabled: Boolean) {
        val current = passcodeRepository.monitorPasscodeType().first() ?: TODO()
        if (enabled && !(isBiometric(current))) {
            passcodeRepository.setPasscodeType(PasscodeType.Biometric(current))
        } else if (!enabled && isBiometric(current)) {
            passcodeRepository.setPasscodeType(current.fallback)
        }
    }

    private fun isBiometric(current: PasscodeType): Boolean {
        contract { returns(true) implies (current is PasscodeType.Biometric) }
        return current is PasscodeType.Biometric
    }
}