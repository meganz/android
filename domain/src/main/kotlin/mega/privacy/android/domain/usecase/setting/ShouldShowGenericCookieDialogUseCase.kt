package mega.privacy.android.domain.usecase.setting

import javax.inject.Inject

/**
 * Use Case to check if the generic cookie dialog should be shown.
 */
class ShouldShowGenericCookieDialogUseCase @Inject constructor(
    private val checkCookieBannerEnabledUseCase: CheckCookieBannerEnabledUseCase,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
) {

    /**
     * Check if the generic cookie dialog should be shown.
     *
     * @return True if the generic cookie dialog should be shown, false otherwise.
     */
    suspend operator fun invoke() =
        checkCookieBannerEnabledUseCase() && getCookieSettingsUseCase().isEmpty()
}