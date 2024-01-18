package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import javax.inject.Inject

/**
 * Use Case to get the type of cookie dialog to be shown.
 */
class GetCookieDialogTypeUseCase @Inject constructor(
    private val shouldShowCookieDialogWithAdsUseCase: ShouldShowCookieDialogWithAdsUseCase,
    private val shouldShowGenericCookieDialogUseCase: ShouldShowGenericCookieDialogUseCase,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
) {
    /**
     *  Get the type of cookie dialog to be shown.
     *
     * @param inAppAdvertisementFeature Feature flag to check if in-app ads are enabled.
     * @param isAdsEnabledFeature Feature flag to check if ads are enabled.
     * @param isExternalAdsEnabledFeature Feature flag to check if external ads are enabled.
     * @return Type of cookie dialog to be shown.
     */
    suspend operator fun invoke(
        inAppAdvertisementFeature: Feature,
        isAdsEnabledFeature: ABTestFeature,
        isExternalAdsEnabledFeature: ABTestFeature,
    ): CookieDialogType {
        val cookieSettings = getCookieSettingsUseCase()

        val shouldShowCookieDialogWithAds = shouldShowCookieDialogWithAdsUseCase(
            cookieSettings,
            inAppAdvertisementFeature,
            isAdsEnabledFeature,
            isExternalAdsEnabledFeature
        )

        return if (shouldShowCookieDialogWithAds) {
            //ADVERTISEMENT cookie is not set, so we need to set it to false
            if (!cookieSettings.contains(CookieType.ADS_CHECK) &&
                cookieSettings.contains(CookieType.ADVERTISEMENT)
            ) {
                updateCookieSettingsUseCase(cookieSettings - CookieType.ADVERTISEMENT)
            }
            CookieDialogType.CookieDialogWithAds
        } else {
            val shouldShowGenericCookieDialog = shouldShowGenericCookieDialogUseCase(cookieSettings)
            if (shouldShowGenericCookieDialog) {
                CookieDialogType.GenericCookieDialog
            } else {
                CookieDialogType.None
            }
        }
    }
}