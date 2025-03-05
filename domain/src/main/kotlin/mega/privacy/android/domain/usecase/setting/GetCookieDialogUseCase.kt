package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.cookie.CookieDialog
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
import javax.inject.Inject

/**
 * Use Case to get the type of cookie dialog to be shown.
 */
class GetCookieDialogUseCase @Inject constructor(
    private val shouldShowGenericCookieDialogUseCase: ShouldShowGenericCookieDialogUseCase,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
) {
    /**
     *  Get the type of cookie dialog to be shown.
     *
     * @return Type of cookie dialog to be shown.
     */
    suspend operator fun invoke(): CookieDialog {
        val cookieSettings = getCookieSettingsUseCase()
        val shouldShowGenericCookieDialog = shouldShowGenericCookieDialogUseCase(cookieSettings)
        return if (shouldShowGenericCookieDialog) {
            CookieDialog(CookieDialogType.GenericCookieDialog, "https://mega.nz/cookie")
        } else {
            CookieDialog(CookieDialogType.None)
        }
    }
}