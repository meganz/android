package mega.privacy.android.domain.usecase

/**
 * Use Case to reset the total uploads
 */
fun interface ResetTotalUploads {

    /**
     * Use Case invocation
     */
    suspend operator fun invoke()
}