package mega.privacy.android.domain.usecase.passcode

import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.entity.passcode.SetPasscodeRequest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import javax.inject.Inject

/**
 * Set passcode use case
 *
 * @property passcodeRepository
 */
class SetPasscodeUseCase @Inject constructor(private val passcodeRepository: PasscodeRepository) {
    /**
     * Invoke
     *
     * @param setPasscodeRequest
     */
    suspend operator fun invoke(setPasscodeRequest: SetPasscodeRequest) {
        val passcode = setPasscodeRequest.passcode
        if (passcode.isBlank()) throw IllegalArgumentException("Passcode cannot be blank")

        val type = setPasscodeRequest.type
        verifyType(type, passcode)

        passcodeRepository.setPasscodeType(type)
        passcodeRepository.setPasscode(passcode)
    }

    private fun verifyType(
        type: PasscodeType,
        passcode: String,
    ) {
        if (type is PasscodeType.Pin) {
            if (type.digits != passcode.length) throw IllegalArgumentException(
                "Passcode PIN digits count do not match number of digits specified in type"
            )
        }
        if (type is PasscodeType.Biometric) verifyType(type.fallback, passcode)
    }
}