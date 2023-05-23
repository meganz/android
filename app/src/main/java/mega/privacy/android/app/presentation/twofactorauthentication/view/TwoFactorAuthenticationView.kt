package mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.view.Constants
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.InitialisationScreen
import mega.privacy.android.core.ui.controls.SimpleTopAppBar
import mega.privacy.android.core.ui.theme.extensions.black_white

@Composable
fun TwoFactorAuthenticationView(
    uiState: TwoFactorAuthenticationUIState,
    onSetupBeginClicked: () -> Unit,

    ) {

    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    Scaffold(
        scaffoldState = rememberScaffoldState(),
        backgroundColor = MaterialTheme.colors.primary,
        topBar = {
            SimpleTopAppBar(
                titleId = R.string.settings_2fa,
                elevation = scrollState.value > 0,
                onBackPressed = {
                    onBackPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(Constants.SNACKBAR_TEST_TAG),
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.black_white
                )
            }
        }
    )
    { padding ->
        InitialisationScreen(onSetupBeginClicked)
    }
}




