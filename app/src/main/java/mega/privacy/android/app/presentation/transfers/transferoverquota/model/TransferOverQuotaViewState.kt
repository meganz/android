package mega.privacy.android.app.presentation.transfers.transferoverquota.model

import kotlin.time.Duration

/**
 * Data class defining the state of the Transfer Over Quota view
 *
 * @property isLoggedIn True if the user is logged in, false otherwise.
 * @property isFreeAccount True if the user has a free account, false otherwise.
 * @property bandwidthOverQuotaDelay Duration in which transfers will be stopped due to a bandwidth over quota.
 */
data class TransferOverQuotaViewState(
    val isLoggedIn: Boolean = true,
    val isFreeAccount: Boolean = true,
    val bandwidthOverQuotaDelay: Duration? = null,
)