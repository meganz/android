package mega.privacy.android.domain.usecase

/**
 * Get current Country code
 */
fun interface GetCurrentCountryCode {
    /**
     * invoke
     */
    suspend operator fun invoke(): String?
}
