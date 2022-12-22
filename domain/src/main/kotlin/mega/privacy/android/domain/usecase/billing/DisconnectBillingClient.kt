package mega.privacy.android.domain.usecase.billing

/**
 * Disconnect billing client
 *
 */
fun interface DisconnectBillingClient {
    suspend operator fun invoke()
}