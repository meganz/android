package mega.privacy.android.domain.usecase

/**
 * Get user's Password Strength
 */
fun interface GetPasswordStrength {
    /**
     * Invoke
     * @param password as password to test
     * @return password strength level from 0 - 4,
     * 0 being the weakest and 4 being the strongest [Int]
     */
    suspend operator fun invoke(password: String): Int
}