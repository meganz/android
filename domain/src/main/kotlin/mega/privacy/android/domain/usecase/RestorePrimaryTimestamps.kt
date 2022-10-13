package mega.privacy.android.domain.usecase

/**
 * If the handle matches the previous primary folder's handle, restore the time stamp from stamps
 */
fun interface RestorePrimaryTimestamps {
    /**
     * Invoke.
     */
    suspend operator fun invoke()
}