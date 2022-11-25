package mega.privacy.android.domain.usecase

/**
 * Reset time stamps for primary media
 */
interface ResetPrimaryTimeline {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke()
}
