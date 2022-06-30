package mega.privacy.android.app.domain.usecase

/**
 * Use case for getting the number of pending uploads.
 */
fun interface GetNumPendingUploads {

    /**
     * Invoke.
     *
     * @return The number of pending downloads  that are not background transfers.
     */
    suspend operator fun invoke(): Int
}