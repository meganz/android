package mega.privacy.android.domain.usecase.transfers

/**
 * Broadcast transfer over quota
 *
 */
fun interface BroadcastTransferOverQuota {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}