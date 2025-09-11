package mega.privacy.android.app.consent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.app.R
import mega.privacy.android.app.consent.model.CookieConsentState
import mega.privacy.android.app.consent.view.CookieConsentDialog
import mega.privacy.android.navigation.destination.CookieSettings
import mega.privacy.android.navigation.destination.WebSite

@Serializable
data object CookieDialog : NavKey

fun NavGraphBuilder.cookieDialogDestination(
    navigateBack: () -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    dialog<CookieDialog> {
        val viewmodel = hiltViewModel<CookieConsentViewModel>()
        val uiState by viewmodel.state.collectAsStateWithLifecycle()

        val coroutineScope = rememberCoroutineScope()
        val snackBarHostState = LocalSnackBarHostState.current
        val cookiesUpdatedMessage = stringResource(id = R.string.dialog_cookie_snackbar_saved)

        when (val state = uiState) {
            is CookieConsentState.Loading -> {
                //No-op
            }

            is CookieConsentState.Data -> {
                CookieConsentDialog(
                    closeDialog = {
                        onDialogHandled()
                        navigateBack()
                    },
                    onAcceptCookies = {
                        viewmodel.acceptAllCookies()
                        coroutineScope.launch {
                            snackBarHostState?.showSnackbar(
                                message = cookiesUpdatedMessage,
                            )
                        }
                    },
                    onAcceptEssentialCookies = {
                        viewmodel.acceptEssentialCookies()
                        coroutineScope.launch {
                            snackBarHostState?.showSnackbar(
                                message = cookiesUpdatedMessage,
                            )
                        }
                    },
                    onOpenCookieSettings = {
                        navigate(CookieSettings)
                    },
                    onNavigateToCookiePolicy = {
                        navigate(WebSite(state.cookiesUrl))
                    },
                )
            }
        }
    }
}