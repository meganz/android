package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Use Case to check if the cookie dialog should be shown with ads.
 */
class ShouldShowCookieDialogWithAdsUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    /**
     *  Check if the cookie dialog should be shown with ads.
     *
     * @param cookieSettings Cookie settings.
     * @param isExternalAdsEnabledFeature Feature flag to check if external ads are enabled.
     * @return True if cookie dialog should be shown with ads, false otherwise.
     */
    suspend operator fun invoke(
        cookieSettings: Set<CookieType>,
        isExternalAdsEnabledFeature: ApiFeature,
    ): Boolean = coroutineScope {
        getFeatureFlagValueUseCase(isExternalAdsEnabledFeature)
                && !cookieSettings.contains(CookieType.ADS_CHECK)
    }
}