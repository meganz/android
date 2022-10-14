package mega.privacy.android.domain.usecase

/**
 * Restore Primary Folder Sync Timestamps
 */
fun interface RestorePrimaryTimestamps {
    /**
     * Invoke
     */
    suspend operator fun invoke()
}
