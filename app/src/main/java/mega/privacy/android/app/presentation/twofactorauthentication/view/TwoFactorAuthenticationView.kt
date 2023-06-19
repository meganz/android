package mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.view.Constants
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.toSeedArray
import mega.privacy.android.app.presentation.twofactorauthentication.model.ScreenType
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.InitialisationScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.SetupScreen
import mega.privacy.android.core.ui.controls.SimpleTopAppBar
import mega.privacy.android.core.ui.theme.extensions.black_white

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TwoFactorAuthenticationView(
    uiState: TwoFactorAuthenticationUIState,
    isDarkMode: Boolean,
    qrCodeMapper: QRCodeMapper,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onFinishActivity: () -> Unit,
    openPlayStore: () -> Unit,
    onOpenInClicked: (String) -> Unit,
) {

    val currentScreen = remember { mutableStateOf(ScreenType.InitialisationScreen) }
    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            when (currentScreen.value) {

                ScreenType.SetupScreen -> {
                    currentScreen.value = ScreenType.InitialisationScreen
                }

                ScreenType.VerificationScreen -> {
                    currentScreen.value = ScreenType.SetupScreen
                }

                else -> {
                    onFinishActivity()
                }
            }
        }

    })
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
        val seedErrorMessage = stringResource(id = R.string.qr_seed_text_error)

        val onBeginSetupClicked: () -> Unit = {
            if (uiState.is2FAFetchCompleted && uiState.seed.isNullOrEmpty()) {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(seedErrorMessage)
                }
            } else {
                currentScreen.value = ScreenType.SetupScreen
            }
        }
        when (currentScreen.value) {
            ScreenType.InitialisationScreen -> {
                InitialisationScreen(
                    onNextClicked = onBeginSetupClicked,
                    modifier = Modifier.semantics { testTagsAsResourceId = true })
            }

            ScreenType.SetupScreen -> {
                SetupScreen(
                    is2FAFetchCompleted = uiState.is2FAFetchCompleted,
                    isDarkMode = isDarkMode,
                    qrText = uiState.twoFactorAuthUrl,
                    qrCodeMapper = qrCodeMapper,
                    seedsList = uiState.seed?.toSeedArray(),
                    onNextClicked = {
                        currentScreen.value = ScreenType.VerificationScreen
                    },
                    openPlayStore = openPlayStore,
                    onOpenInClicked = onOpenInClicked,
                    modifier = Modifier.semantics { testTagsAsResourceId = true }
                )
            }

            ScreenType.VerificationScreen -> {

            }

            ScreenType.VerificationPassedScreen -> {

            }
        }

    }
}