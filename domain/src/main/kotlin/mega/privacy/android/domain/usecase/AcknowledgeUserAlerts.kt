package mega.privacy.android.domain.usecase

/**
 * Acknowledge user alerts use case
 *
 */
fun interface AcknowledgeUserAlerts {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}