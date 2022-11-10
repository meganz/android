package mega.privacy.android.domain.usecase

/**
 * Broadcast Camera Upload Pause State
 */
fun interface BroadcastUploadPauseState {
    /**
     * Invoke
     */
    suspend operator fun invoke()
}