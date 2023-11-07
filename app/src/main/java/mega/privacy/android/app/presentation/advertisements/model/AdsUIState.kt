package mega.privacy.android.app.presentation.advertisements.model

/**
 * UI state for the [AdsBannerView]
 * @param showAdsView UI state to change the visibility of the Ad view
 * @param slotId The slot ID of the currently displayed Ad in the Ads view
 * @param adsBannerUrl the url used to load the Ad webview banner
 */
data class AdsUIState(
    val showAdsView: Boolean = false,
    val slotId: String = "",
    val adsBannerUrl: String = "",
)
