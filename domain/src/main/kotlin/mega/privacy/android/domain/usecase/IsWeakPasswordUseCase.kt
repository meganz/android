package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import javax.inject.Inject
import kotlin.text.isNullOrBlank

class IsWeakPasswordUseCase @Inject constructor(
    private val getPasswordStrengthUseCase: GetPasswordStrengthUseCase,
) {
    suspend operator fun invoke(password: String?): Boolean =
        password.isNullOrBlank() || isPasswordWeak(
            passwordStrength = getPasswordStrengthUseCase(
                password
            )
        )

    private fun isPasswordWeak(passwordStrength: PasswordStrength) =
        passwordStrength == PasswordStrength.INVALID || passwordStrength == PasswordStrength.VERY_WEAK || passwordStrength == PasswordStrength.WEAK
}