package mega.privacy.android.domain.usecase

/**
 * Use Case to check if there are Pending Uploads or not
 */
fun interface HasPendingUploads {

    /**
     * Checks whether there are pending uploads or not
     *
     * @return true if there are pending uploads, and false if otherwise
     */
    suspend operator fun invoke(): Boolean
}