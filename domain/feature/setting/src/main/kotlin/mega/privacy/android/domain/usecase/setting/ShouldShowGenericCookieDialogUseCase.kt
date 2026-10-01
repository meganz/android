package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.cookie.CookieType
import javax.inject.Inject

/**
 * Use Case to check if the generic cookie dialog should be shown.
 */
class ShouldShowGenericCookieDialogUseCase @Inject constructor(
    private val checkCookieBannerEnabledUseCase: CheckCookieBannerEnabledUseCase,
) {

    /**
     * Check if the generic cookie dialog should be shown.
     * @param cookieSettings    Set of cookie settings.
     * @return True if the generic cookie dialog should be shown, false otherwise.
     */
    suspend operator fun invoke(cookieSettings: Set<CookieType>) =
        checkCookieBannerEnabledUseCase() && cookieSettings.isEmpty()
}