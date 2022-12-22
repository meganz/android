package mega.privacy.android.app.domain.usecase

/**
 * Use Case to cancel all upload transfers
 */
fun interface CancelAllUploadTransfers {

    /**
     * Cancel all upload transfers
     */
    suspend operator fun invoke()
}