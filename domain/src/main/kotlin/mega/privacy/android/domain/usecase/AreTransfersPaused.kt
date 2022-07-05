package mega.privacy.android.domain.usecase

/**
 * The use case interface to get paused transfers boolean flag
 */
fun interface AreTransfersPaused {
    /**
     * Are transfers paused (downloads and uploads)
     */
    suspend operator fun invoke(): Boolean
}
