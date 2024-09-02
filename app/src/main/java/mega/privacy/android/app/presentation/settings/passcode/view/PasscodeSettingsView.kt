package mega.privacy.android.app.presentation.settings.passcode.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.passcode.model.PasscodeSettingsUIState
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.app.presentation.settings.passcode.view.tile.ChangePasscodeTile
import mega.privacy.android.app.presentation.settings.passcode.view.tile.EnablePasscodeTile
import mega.privacy.android.app.presentation.settings.passcode.view.tile.FingerprintIdTile
import mega.privacy.android.app.presentation.settings.passcode.view.tile.RequirePasscodeTile
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PasscodeSettingsView(
    state: PasscodeSettingsUIState,
    onDisablePasscode: () -> Unit,
    onDisableBiometrics: () -> Unit,
    navigateToChangePasscode: () -> Unit,
    navigateToSelectTimeout: () -> Unit,
    hasBiometricCapability: Boolean,
    authenticateBiometrics: @Composable (onSuccess: () -> Unit, onComplete: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberScaffoldState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showBiometricPrompt by remember {
        mutableStateOf(false)
    }

    MegaScaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(PASSCODE_SETTINGS_TOOLBAR),
                title = stringResource(R.string.settings_passcode_lock),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
            ) {
                EnablePasscodeTile(
                    isChecked = state.isEnabled,
                    onItemClicked = {
                        if (state.isEnabled) onDisablePasscode() else navigateToChangePasscode()
                    }
                )
                if (state.isEnabled) {
                    ChangePasscodeTile(
                        onItemClicked = navigateToChangePasscode
                    )
                    if (hasBiometricCapability) {
                        FingerprintIdTile(
                            isChecked = state.isBiometricsEnabled,
                            onItemClicked = {
                                if (state.isBiometricsEnabled) {
                                    onDisableBiometrics()
                                } else {
                                    showBiometricPrompt = true
                                }
                            }
                        )
                    }
                    RequirePasscodeTile(
                        onItemClicked = navigateToSelectTimeout,
                        subTitle = state.timeout?.getTitle(context)
                    )
                }
            }

            if (showBiometricPrompt) {
                val message = stringResource(id = R.string.confirmation_fingerprint_enabled)
                authenticateBiometrics(
                    {
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(message)
                        }
                        showBiometricPrompt = false
                    },
                    {
                        showBiometricPrompt = false
                    }
                )
            }
        },
    )
}


@CombinedThemePreviews
@Composable
private fun PasscodeSettingsViewPreview(
    @PreviewParameter(PasscodeSettingsViewPreviewProvider::class) params: Pair<Boolean, Boolean>,
) {
    var state by remember {
        mutableStateOf(
            PasscodeSettingsUIState(
                isEnabled = params.first,
                isBiometricsEnabled = params.first,
                timeout = TimeoutOption.Immediate
            )
        )
    }

    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PasscodeSettingsView(
            state = state,
            onDisablePasscode = {
                state = state.copy(isEnabled = !state.isEnabled)
            },
            onDisableBiometrics = {
                state = state.copy(isBiometricsEnabled = !state.isBiometricsEnabled)
            },
            navigateToChangePasscode = {},
            authenticateBiometrics = { _, _ -> },
            navigateToSelectTimeout = {},
            hasBiometricCapability = params.second,
        )
    }
}

private class PasscodeSettingsViewPreviewProvider :
    PreviewParameterProvider<Pair<Boolean, Boolean>> {
    override val values: Sequence<Pair<Boolean, Boolean>> = listOf(
        true to true,
        true to false,
        false to true,
        false to false,
    ).asSequence()
}

internal const val PASSCODE_SETTINGS_TOOLBAR = "passcode_settings_view:mega_app_bar"