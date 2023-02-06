package mega.privacy.android.domain.usecase

/**
 * Checks if user session exists.
 */
fun interface GetSession {
    /**
     * Invoke
     *
     * @return session if exists.
     */
    suspend operator fun invoke(): String?
}