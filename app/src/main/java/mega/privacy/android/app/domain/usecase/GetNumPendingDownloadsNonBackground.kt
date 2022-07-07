package mega.privacy.android.app.domain.usecase

/**
 * Use case for getting the number of pending download transfers that are not background transfers.
 */
fun interface GetNumPendingDownloadsNonBackground {

    /**
     * Invoke.
     *
     * @return The number of pending downloads  that are not background transfers.
     */
    suspend operator fun invoke(): Int
}