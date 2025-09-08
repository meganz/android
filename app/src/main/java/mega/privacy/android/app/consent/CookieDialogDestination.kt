package mega.privacy.android.app.consent

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.consent.view.CookieConsentDialog

@Serializable
data object CookieDialog : NavKey

fun NavGraphBuilder.cookieDialogDestination(
    navigateBack: () -> Unit,
    navigateAndClear: (NavKey, NavKey, Boolean) -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    dialog<CookieDialog> {
        CookieConsentDialog(
            closeDialog = { },
            onAcceptCookies = { },
            onAcceptEssentialCookies = { },
            onOpenCookieSettings = { },
            onNavigateToCookiePolicy = {},
        )
    }
}