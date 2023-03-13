package mega.privacy.android.domain.usecase.transfer

/**
 * Use case for checking if there are ongoing transfers.
 */
fun interface ExistOngoingTransfers {

    /**
     * Invoke.
     *
     * @return True if there are ongoing transfers, false otherwise.
     */
    suspend operator fun invoke(): Boolean
}