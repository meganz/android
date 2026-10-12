package mega.privacy.mobile.home.presentation.home.widget.banner.model

import mega.privacy.android.domain.entity.banner.PromotionalBanner

/**
 * UI state for banner widget
 *
 * @property offerBanner The locally-built subscription offer banner, shown ahead of [banners]; null
 * when there is no active offer or it has been dismissed
 * @property banners List of promotional banners delivered by the banners API
 * @property isLoading Whether banners are currently being loaded
 * @property isOfflineMode Whether the app is in offline mode; banner actions need a connection
 */
data class BannerUiState(
    val offerBanner: SubscriptionOfferBannerUiModel? = null,
    val banners: List<PromotionalBanner> = emptyList(),
    val isLoading: Boolean = false,
    val isOfflineMode: Boolean = false,
)
