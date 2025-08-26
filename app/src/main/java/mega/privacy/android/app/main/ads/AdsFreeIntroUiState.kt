package mega.privacy.android.app.main.ads

import mega.privacy.android.feature.payment.model.LocalisedSubscription

/**
 * Ads Free Intro UI state
 *
 * @property cheapestSubscriptionAvailable the cheapest subscription available
 */
data class AdsFreeIntroUiState(
    val cheapestSubscriptionAvailable: LocalisedSubscription? = null,
)