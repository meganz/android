package mega.privacy.android.domain.usecase

/**
 * Use case to return the multi factor authentication code
 */
fun interface GetMultiFactorAuthCode {

    /**
     * Invoke
     * @return the secret code of the account to enable multi-factor authentication [String]
     */
    suspend operator fun invoke(): String
}