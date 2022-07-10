package mega.privacy.android.domain.usecase

/**
 * Checks if user credentials exists.
 */
fun interface GetSession {
    /**
     * Invoke
     *
     * @return [UserCredentials] if exists.
     */
    suspend operator fun invoke(): String?
}