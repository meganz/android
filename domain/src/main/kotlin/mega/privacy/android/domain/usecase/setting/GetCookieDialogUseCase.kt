package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.settings.cookie.CookieDialog
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import javax.inject.Inject

/**
 * Use Case to get the type of cookie dialog to be shown.
 */
class GetCookieDialogUseCase @Inject constructor(
    private val shouldShowCookieDialogWithAdsUseCase: ShouldShowCookieDialogWithAdsUseCase,
    private val shouldShowGenericCookieDialogUseCase: ShouldShowGenericCookieDialogUseCase,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val updateCookieSettingsUseCase: UpdateCookieSettingsUseCase,
    private val getSessionTransferURLUseCase: GetSessionTransferURLUseCase,
) {
    /**
     *  Get the type of cookie dialog to be shown.
     *
     * @param isExternalAdsEnabledFeature Feature flag to check if external ads are enabled.
     * @return Type of cookie dialog to be shown.
     */
    suspend operator fun invoke(
        isExternalAdsEnabledFeature: ApiFeature,
    ): CookieDialog {
        val cookieSettings = getCookieSettingsUseCase()

        val shouldShowCookieDialogWithAds = shouldShowCookieDialogWithAdsUseCase(
            cookieSettings,
            isExternalAdsEnabledFeature
        )

        return if (shouldShowCookieDialogWithAds) {
            //ADVERTISEMENT cookie is not set, so we need to set it to false
            if (!cookieSettings.contains(CookieType.ADS_CHECK) &&
                cookieSettings.contains(CookieType.ADVERTISEMENT)
            ) {
                updateCookieSettingsUseCase(cookieSettings - CookieType.ADVERTISEMENT)
            }
            val cookiePolicyLink = getSessionTransferURLUseCase("cookie")
            CookieDialog(CookieDialogType.CookieDialogWithAds, cookiePolicyLink)
        } else {
            val shouldShowGenericCookieDialog = shouldShowGenericCookieDialogUseCase(cookieSettings)
            if (shouldShowGenericCookieDialog) {
                CookieDialog(CookieDialogType.GenericCookieDialog, "https://mega.nz/cookie")
            } else {
                CookieDialog(CookieDialogType.None)
            }
        }
    }
}