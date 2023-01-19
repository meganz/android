package mega.privacy.android.domain.usecase

/**
 * The use case interface to cancel transfer by Tag
 */
fun interface CancelTransferByTag {
    /**
     * Invoke
     * @param transferTag
     */
    suspend operator fun invoke(transferTag: Int)
}