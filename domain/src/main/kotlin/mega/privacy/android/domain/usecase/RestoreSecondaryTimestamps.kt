package mega.privacy.android.domain.usecase

/**
 * Restore Secondary Folder Sync Timestamps
 */
fun interface RestoreSecondaryTimestamps {
    /**
     * Invoke
     */
    suspend operator fun invoke()
}
