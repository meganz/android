package mega.privacy.android.app.presentation.advertisements

import com.google.android.gms.ads.admanager.AdManagerAdRequest

data class AdsUiState(
    val request: AdManagerAdRequest? = null,
    val isAdsFeatureEnabled: Boolean? = null,
)