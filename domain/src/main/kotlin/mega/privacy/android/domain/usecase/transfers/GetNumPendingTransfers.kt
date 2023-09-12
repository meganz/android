package mega.privacy.android.domain.usecase.transfers

/**
 * Use case for getting the number of pending transfers.
 * For downloads only takes into account those which are not background ones.
 */
fun interface GetNumPendingTransfers {

    /**
     * Invoke.
     *
     * @return Number of pending transfers.
     */
    suspend operator fun invoke(): Int
}