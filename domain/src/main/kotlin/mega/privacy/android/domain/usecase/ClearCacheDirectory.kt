package mega.privacy.android.domain.usecase

/**
 * Clear Cache Directory
 */
fun interface ClearCacheDirectory {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}