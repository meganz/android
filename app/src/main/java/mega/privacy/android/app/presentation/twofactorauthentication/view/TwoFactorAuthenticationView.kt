package mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.ScreenType
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationCompletedScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationSetupScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.InitialisationScreen
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TwoFactorAuthenticationView(
    uiState: TwoFactorAuthenticationUIState,
    isDarkMode: Boolean,
    qrCodeMapper: QRCodeMapper,
    onFinishActivity: () -> Unit,
    openPlayStore: () -> Unit,
    isIntentAvailable: (String) -> Boolean,
    onOpenInClicked: (String) -> Unit,
    on2FAChanged: (String) -> Unit,
    on2FAPinReset: () -> Unit,
    onExportRkClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    onCopySeedLongClicked: () -> Unit,
    onIsRkExportSuccessfullyConsumed: () -> Unit,
    onIsWritePermissionDeniedConsumed: () -> Unit,
    onIsSeedCopiedToClipboardConsumed: () -> Unit,
) {
    var currentScreen by rememberSaveable {
        mutableStateOf(ScreenType.InitialisationScreen)
    }
    val resources = LocalResources.current
    val scaffoldState = rememberScaffoldState()
    val snackBarHostState = scaffoldState.snackbarHostState
    val coroutineScope = rememberCoroutineScope()

    val onBack: () -> Unit = {
        when (currentScreen) {
            ScreenType.SetupScreen -> {
                currentScreen = ScreenType.InitialisationScreen
            }

            ScreenType.AuthenticationScreen -> {
                on2FAPinReset()
                currentScreen = ScreenType.SetupScreen
            }

            else -> {
                onFinishActivity()
            }
        }
    }
    BackHandler(onBack = onBack)

    MegaScaffold(
        modifier = Modifier.imePadding(),
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(id = R.string.settings_2fa),
                onNavigationPressed = onBack,
            )
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        val onBeginSetupClicked: () -> Unit = {
            if (uiState.is2FAFetchCompleted && uiState.seed.isNullOrEmpty()) {
                coroutineScope.launch {
                    snackBarHostState.showAutoDurationSnackbar(resources.getString(R.string.qr_seed_text_error))
                }
            } else {
                currentScreen = ScreenType.SetupScreen
            }
        }
        when (currentScreen) {
            ScreenType.InitialisationScreen -> {
                InitialisationScreen(
                    onNextClicked = onBeginSetupClicked,
                    modifier = contentModifier.semantics { testTagsAsResourceId = true },
                )
            }

            ScreenType.SetupScreen -> {
                AuthenticationSetupScreen(
                    uiState = uiState,
                    isDarkMode = isDarkMode,
                    qrCodeMapper = qrCodeMapper,
                    onNextClicked = {
                        currentScreen = ScreenType.AuthenticationScreen
                    },
                    openPlayStore = openPlayStore,
                    isIntentAvailable = isIntentAvailable,
                    onOpenInClicked = onOpenInClicked,
                    onCopySeedLongClicked = onCopySeedLongClicked,
                    modifier = contentModifier.semantics { testTagsAsResourceId = true },
                )
            }

            ScreenType.AuthenticationScreen -> {
                AuthenticationScreen(
                    uiState = uiState,
                    on2FAChanged = on2FAChanged,
                    modifier = contentModifier,
                )
            }

            ScreenType.AuthenticationCompletedScreen -> {
                AuthenticationCompletedScreen(
                    isMasterKeyExported = uiState.isMasterKeyExported,
                    onExportRkClicked = onExportRkClicked,
                    onDismissClicked = onDismissClicked,
                    modifier = contentModifier,
                )
            }
        }

        EventEffect(
            event = uiState.seedCopiedToClipboardEvent,
            onConsumed = onIsSeedCopiedToClipboardConsumed
        ) {
            snackBarHostState.showAutoDurationSnackbar(resources.getString(R.string.messages_copied_clipboard))
        }

        EventEffect(
            event = uiState.writePermissionDeniedEvent,
            onConsumed = onIsWritePermissionDeniedConsumed
        ) {
            snackBarHostState.showAutoDurationSnackbar(resources.getString(R.string.denied_write_permissions))
        }

        EventEffect(
            event = uiState.isRkExportedSuccessfullyEvent,
            onConsumed = onIsRkExportSuccessfullyConsumed
        ) { isExported ->
            snackBarHostState.showAutoDurationSnackbar(
                resources.getString(
                    if (isExported) R.string.save_MK_confirmation
                    else sharedR.string.general_text_error
                )
            )
        }

        when (uiState.authenticationState) {
            AuthenticationState.Passed -> {
                currentScreen = ScreenType.AuthenticationCompletedScreen
            }

            AuthenticationState.Error,
                -> {
                LaunchedEffect(key1 = uiState.authenticationState) {
                    snackBarHostState.showAutoDurationSnackbar(resources.getString(R.string.error_enable_2fa))
                }
            }

            else -> {}
        }
    }
}