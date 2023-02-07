package mega.privacy.android.domain.usecase.transfer

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