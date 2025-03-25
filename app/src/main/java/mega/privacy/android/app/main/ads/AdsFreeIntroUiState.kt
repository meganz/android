package mega.privacy.android.app.main.ads

import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription

/**
 * Ads Free Intro UI state
 *
 * @property cheapestSubscriptionAvailable the cheapest subscription available
 */
data class AdsFreeIntroUiState(
    val cheapestSubscriptionAvailable: LocalisedSubscription? = null,
)