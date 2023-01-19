package mega.privacy.android.domain.usecase

/**
 * Get Number of Pending Uploads
 */
fun interface GetNumberOfPendingUploads {
    /**
     * Invoke
     *
     * @return number of pending uploads
     */
    operator fun invoke(): Int
}
