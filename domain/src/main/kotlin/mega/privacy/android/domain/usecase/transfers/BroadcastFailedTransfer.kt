package mega.privacy.android.domain.usecase.transfers

/**
 * Broadcast failed transfer
 *
 */
fun interface BroadcastFailedTransfer {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(isFailed: Boolean)
}