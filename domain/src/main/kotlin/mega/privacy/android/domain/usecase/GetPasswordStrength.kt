package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * Get user's Password Strength
 */
fun interface GetPasswordStrength {
    /**
     * Invoke
     * @param password as password to test
     * @return password strength as [PasswordStrength] attribute
     */
    suspend operator fun invoke(password: String): PasswordStrength
}