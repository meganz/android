package mega.privacy.android.domain.usecase

/**
 * Broadcast Finish Activity
 *
 */
fun interface BroadcastFinishActivity {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}