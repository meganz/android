package mega.privacy.android.shared.ads.advertisements

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest

data class AdsUiState(
    val request: BannerAdRequest? = null,
    val isAdsFeatureEnabled: Boolean? = null,
)
