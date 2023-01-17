package mega.privacy.android.domain.usecase

/**
 * The use case for credentials whether is null
 */
fun interface CredentialsIsNull {

    /**
     * Credentials whether is null
     *
     * @return true is null, otherwise is false
     */
    suspend operator fun invoke(): Boolean
}