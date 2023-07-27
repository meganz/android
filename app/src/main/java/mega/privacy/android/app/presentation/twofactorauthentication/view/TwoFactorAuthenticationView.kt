package mega.privacy.android.app.presentation.twofactorauthentication.view

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.view.Constants
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.ScreenType
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationCompletedScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.InitialisationScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationSetupScreen
import mega.privacy.android.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.core.ui.theme.extensions.black_white

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TwoFactorAuthenticationView(
    uiState: TwoFactorAuthenticationUIState,
    isDarkMode: Boolean,
    qrCodeMapper: QRCodeMapper,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onFinishActivity: () -> Unit,
    openPlayStore: () -> Unit,
    isIntentAvailable: (String) -> Boolean,
    onOpenInClicked: (String) -> Unit,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    onFirstTime2FAConsumed: () -> Unit,
    on2FAPinReset: () -> Unit,
    onExportRkClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onCopySeedLongClicked: () -> Unit,
    onIsRkExportSuccessfullyConsumed: () -> Unit,
    onIsWritePermissionDeniedConsumed: () -> Unit,
    onIsSeedCopiedToClipboardConsumed: () -> Unit,
) {

    val currentScreen =
        remember { mutableStateOf(ScreenType.InitialisationScreen) }

    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(enabled = true) {
        override fun handleOnBackPressed() {
            when (currentScreen.value) {

                ScreenType.SetupScreen -> {
                    currentScreen.value = ScreenType.InitialisationScreen
                }

                ScreenType.AuthenticationScreen -> {
                    on2FAPinReset()
                    currentScreen.value = ScreenType.SetupScreen
                }

                else -> {
                    onFinishActivity()
                }
            }
        }

    })
    Scaffold(
        modifier = Modifier.imePadding(),
        scaffoldState = scaffoldState,
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
                    backgroundColor = MaterialTheme.colors.black_white,
                )
            }
        }
    )
    { padding ->
        val context = LocalContext.current
        val onBeginSetupClicked: () -> Unit = {
            if (uiState.is2FAFetchCompleted && uiState.seed.isNullOrEmpty()) {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(context.resources.getString(R.string.qr_seed_text_error))
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
                AuthenticationSetupScreen(
                    uiState = uiState,
                    isDarkMode = isDarkMode,
                    qrCodeMapper = qrCodeMapper,
                    onNextClicked = {
                        currentScreen.value = ScreenType.AuthenticationScreen
                    },
                    openPlayStore = openPlayStore,
                    isIntentAvailable = isIntentAvailable,
                    onOpenInClicked = onOpenInClicked,
                    onCopySeedLongClicked = onCopySeedLongClicked,
                    modifier = Modifier.semantics { testTagsAsResourceId = true }
                )
            }

            ScreenType.AuthenticationScreen -> {
                AuthenticationScreen(
                    uiState = uiState,
                    on2FAPinChanged = on2FAPinChanged,
                    on2FAChanged = on2FAChanged,
                    onFirstTime2FAConsumed = onFirstTime2FAConsumed
                )
            }

            ScreenType.AuthenticationCompletedScreen -> {
                AuthenticationCompletedScreen(
                    isMasterKeyExported = uiState.isMasterKeyExported,
                    onExportRkClicked = onExportRkClicked,
                    onDismissClicked = onDismissClicked
                )
            }
        }

        EventEffect(
            event = uiState.seedCopiedToClipboardEvent,
            onConsumed = onIsSeedCopiedToClipboardConsumed
        ) {
            snackBarHostState.showSnackbar(context.resources.getString(R.string.messages_copied_clipboard))
        }

        EventEffect(
            event = uiState.writePermissionDeniedEvent,
            onConsumed = onIsWritePermissionDeniedConsumed
        ) {
            snackBarHostState.showSnackbar(context.resources.getString(R.string.denied_write_permissions))
        }

        EventEffect(
            event = uiState.isRkExportedSuccessfullyEvent,
            onConsumed = onIsRkExportSuccessfullyConsumed
        ) { isExported ->
            if (isExported) {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.save_MK_confirmation),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                snackBarHostState.showSnackbar(context.resources.getString(R.string.general_text_error))
            }
        }

        when (uiState.authenticationState) {
            AuthenticationState.Passed -> {
                currentScreen.value = ScreenType.AuthenticationCompletedScreen
            }

            AuthenticationState.Error,
            -> {
                LaunchedEffect(key1 = uiState.authenticationState) {
                    snackBarHostState.showSnackbar(context.resources.getString(R.string.error_enable_2fa))
                }
            }

            else -> {}
        }
    }
}