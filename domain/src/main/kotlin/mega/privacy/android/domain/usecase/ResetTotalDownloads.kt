package mega.privacy.android.domain.usecase

/**
 * Use case to reset total downloads
 */
fun interface ResetTotalDownloads {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}