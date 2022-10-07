package mega.privacy.android.app.domain.usecase

/**
 * Get secondary camera sync handle
 */
interface GetSecondarySyncHandle {
    /**
     * Invoke
     *
     * @return sync handle
     */
    suspend operator fun invoke(): Long
}
