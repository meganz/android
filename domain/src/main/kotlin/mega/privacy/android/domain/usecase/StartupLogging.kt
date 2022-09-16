package mega.privacy.android.domain.usecase

/**
 * Startup logging
 *
 */
fun interface StartupLogging {
    /**
     * Invoke
     */
    suspend operator fun invoke()
}