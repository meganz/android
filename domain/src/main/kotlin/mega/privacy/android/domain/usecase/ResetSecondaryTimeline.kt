package mega.privacy.android.domain.usecase

/**
 * Reset time stamps for secondary media
 */
interface ResetSecondaryTimeline {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke()
}
