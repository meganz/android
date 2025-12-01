package mega.privacy.mobile.home.presentation.home.widget.banner.model

import mega.privacy.android.domain.entity.banner.Banner

/**
 * UI state for banner widget
 *
 * @property banners List of banners to display
 * @property isLoading Whether banners are currently being loaded
 */
data class BannerUiState(
    val banners: List<Banner> = emptyList(),
    val isLoading: Boolean = false,
)
