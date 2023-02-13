package mega.privacy.android.domain.usecase

/**
 * Use case for cancelling all transfers, uploads and downloads.
 */
fun interface CancelTransfers {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}