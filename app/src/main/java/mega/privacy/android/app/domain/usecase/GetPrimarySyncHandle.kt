package mega.privacy.android.app.domain.usecase

/**
 * Get primary camera sync handle
 */
interface GetPrimarySyncHandle {
    /**
     * Invoke
     *
     * @return sync handle
     */
    suspend operator fun invoke(): Long
}
