package mega.privacy.android.app.domain.usecase

/**
 * Get current time string
 */
fun interface GetCurrentTimeString {
    /**
     * Invoke
     *
     * @param format
     * @return current time as formatted string
     */
    suspend operator fun invoke(format: String, timeZone: String): String
}