package mega.privacy.android.app.presentation.advertisements.model

/**
 * UI state for the [AdsBannerView]
 * @param showAdsView UI state to change the visibility of the Ad view
 * @param adsBannerUrl the url used to load the Ad webview banner
 */
data class AdsUIState(
    val showAdsView: Boolean = false,
    val adsBannerUrl: String = "",
)
