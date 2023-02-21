package mega.privacy.android.app.domain.usecase

/**
 * Get Camera Upload Attributes
 *
 */
fun interface GetCameraUploadAttributes {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Pair<Long, Long>?
}
