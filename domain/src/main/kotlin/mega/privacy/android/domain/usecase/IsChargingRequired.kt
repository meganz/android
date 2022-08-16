package mega.privacy.android.domain.usecase

/**
 * Get is charging required
 *
 */
interface IsChargingRequired {

    /**
     * Invoke
     *
     * @return is charging required
     */
    suspend operator fun invoke(queueSize: Long): Boolean
}
