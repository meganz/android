package mega.privacy.android.app.domain.usecase

/**
 * Use case interface for getting camera sort order
 */
fun interface GetCameraSortOrder {

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend operator fun invoke(): Int
}