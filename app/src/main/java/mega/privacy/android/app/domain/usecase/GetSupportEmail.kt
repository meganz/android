package mega.privacy.android.app.domain.usecase

/**
 * Get the support email address
 *
 */
fun interface GetSupportEmail {
    /**
     * Invoke
     *
     * @return Support email address as string
     */
    suspend operator fun invoke(): String
}