package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Get user's Password Strength
 */
class GetPasswordStrengthUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    /**
     * Invoke
     * @param password as password to test
     * @return password strength as [PasswordStrength] attribute
     */
    suspend operator fun invoke(password: String): PasswordStrength =
        repository.getPasswordStrength(password)
}