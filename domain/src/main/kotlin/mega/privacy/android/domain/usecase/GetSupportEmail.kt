package mega.privacy.android.domain.usecase

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