package mega.privacy.android.domain.usecase

/**
 * Use case to enable multi-factor authentication for the account
 */
fun interface EnableMultiFactorAuth {

    /**
     * Invoke
     * @param pin the valid pin code for multi-factor authentication
     * @return if multi-factor authentication got enabled successfully or not as [Boolean]
     */
    suspend operator fun invoke(pin: String): Boolean
}