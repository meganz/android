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
    operator fun invoke(queueSize: Long): Boolean
}
