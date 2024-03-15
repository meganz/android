package mega.privacy.android.app.presentation.settings.camerauploads

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import de.palm.composestateevents.StateEvent
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.account.business.BusinessAccountSuspendedDialog
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CameraUploadsBusinessAccountDialog
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.HowToUploadDialog
import mega.privacy.android.app.presentation.settings.camerauploads.model.SettingsCameraUploadsState
import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadConnectionType
import mega.privacy.android.app.presentation.settings.camerauploads.permissions.CameraUploadsPermissionsHandler
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.CameraUploadsTile
import mega.privacy.android.app.presentation.settings.camerauploads.tiles.HowToUploadTile
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A Composable that holds views displaying the main Settings Camera Uploads screen
 *
 * @param uiState The Settings Camera Uploads State
 * @param onBusinessAccountAdministratorSuspendedPromptAcknowledged Lambda to execute when the
 * Suspended Business Account Administrator acknowledges that Camera Uploads cannot be enabled until
 * the specific issue has been resolved
 * @param onBusinessAccountPromptAcknowledged Lambda to execute when the User acknowledges that the
 * Business Account Administrator can access his/her Camera Uploads content
 * @param onBusinessAccountPromptDismissed Lambda to execute when the User dismisses the Business
 * Account prompt
 * @param onBusinessAccountSubUserSuspendedPromptAcknowledged Lambda to execute when the
 * Suspended Business Account Sub-User acknowledges that Camera Uploads cannot be enabled until
 * the specific issue has been resolved
 * @param onCameraUploadsStateChanged Lambda to execute when the Camera Uploads state changes
 * @param onHowToUploadPromptOptionSelected Lambda to execute when the User selects a new
 * [UploadConnectionType] from the How to Upload prompt
 * @param onMediaPermissionsGranted Lambda to execute when the User has granted the Media Permissions
 * @param onMediaPermissionsRationaleStateChanged Lambda to execute when the Media Permissions needs
 * to be shown (true) or hidden (false)
 * @param onRequestPermissionsStateChanged Lambda to execute whether a Camera Uploads permissions
 * request should be done (triggered) or not (consumed)
 * @param onSettingsScreenPaused Lambda to execute when the User triggers onPause() in the Settings
 * screen
 */
@Composable
internal fun SettingsCameraUploadsView(
    uiState: SettingsCameraUploadsState,
    onBusinessAccountAdministratorSuspendedPromptAcknowledged: () -> Unit,
    onBusinessAccountPromptAcknowledged: () -> Unit,
    onBusinessAccountPromptDismissed: () -> Unit,
    onBusinessAccountSubUserSuspendedPromptAcknowledged: () -> Unit,
    onCameraUploadsStateChanged: (Boolean) -> Unit,
    onHowToUploadPromptOptionSelected: (UploadConnectionType) -> Unit,
    onMediaPermissionsGranted: () -> Unit,
    onMediaPermissionsRationaleStateChanged: (Boolean) -> Unit,
    onRequestPermissionsStateChanged: (StateEvent) -> Unit,
    onSettingsScreenPaused: () -> Unit,
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var showHowToUploadPrompt by rememberSaveable { mutableStateOf(false) }

    // When the User triggers the onPause Lifecycle Event, check if Camera Uploads can be started
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onSettingsScreenPaused.invoke()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MegaScaffold(
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(SETTINGS_CAMERA_UPLOADS_TOOLBAR),
                title = stringResource(R.string.section_photo_sync),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
            )
        },
        content = { padding ->
            CameraUploadsPermissionsHandler(
                modifier = Modifier.padding(padding),
                requestPermissions = uiState.requestPermissions,
                showMediaPermissionsRationale = uiState.showMediaPermissionsRationale,
                onMediaPermissionsGranted = onMediaPermissionsGranted,
                onMediaPermissionsRationaleStateChanged = onMediaPermissionsRationaleStateChanged,
                onRequestPermissionsStateChanged = onRequestPermissionsStateChanged,
            )
            if (uiState.showBusinessAccountPrompt) {
                CameraUploadsBusinessAccountDialog(
                    onAlertAcknowledged = onBusinessAccountPromptAcknowledged,
                    onAlertDismissed = onBusinessAccountPromptDismissed,
                )
            }
            if (uiState.showBusinessAccountSubUserSuspendedPrompt) {
                BusinessAccountSuspendedDialog(
                    isBusinessAdministratorAccount = false,
                    onAlertAcknowledged = onBusinessAccountSubUserSuspendedPromptAcknowledged,
                    onAlertDismissed = onBusinessAccountSubUserSuspendedPromptAcknowledged,
                )
            }
            if (uiState.showBusinessAccountAdministratorSuspendedPrompt) {
                BusinessAccountSuspendedDialog(
                    isBusinessAdministratorAccount = true,
                    onAlertAcknowledged = onBusinessAccountAdministratorSuspendedPromptAcknowledged,
                    onAlertDismissed = onBusinessAccountAdministratorSuspendedPromptAcknowledged,
                )
            }
            if (showHowToUploadPrompt) {
                HowToUploadDialog(
                    currentUploadConnectionType = uiState.uploadConnectionType,
                    onOptionSelected = { newUploadConnectionType ->
                        showHowToUploadPrompt = false
                        onHowToUploadPromptOptionSelected.invoke(newUploadConnectionType)
                    },
                    onDismissRequest = { showHowToUploadPrompt = false },
                )
            }
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                CameraUploadsTile(
                    isChecked = uiState.isCameraUploadsEnabled,
                    onCheckedChange = onCameraUploadsStateChanged,
                )
                if (uiState.isCameraUploadsEnabled) {
                    HowToUploadTile(
                        uploadConnectionType = uiState.uploadConnectionType,
                        onItemClicked = { showHowToUploadPrompt = true },
                    )
                }
            }
        },
    )
}

/**
 * A Composable Preview for [SettingsCameraUploadsView]
 *
 * @param uiState The [SettingsCameraUploadsState]
 */
@CombinedThemePreviews
@Composable
private fun SettingsCameraUploadsViewPreview(
    @PreviewParameter(SettingsCameraUploadsViewParameterProvider::class) uiState: SettingsCameraUploadsState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SettingsCameraUploadsView(
            uiState = uiState,
            onBusinessAccountAdministratorSuspendedPromptAcknowledged = {},
            onBusinessAccountPromptAcknowledged = {},
            onBusinessAccountPromptDismissed = {},
            onBusinessAccountSubUserSuspendedPromptAcknowledged = {},
            onCameraUploadsStateChanged = {},
            onHowToUploadPromptOptionSelected = {},
            onMediaPermissionsGranted = {},
            onMediaPermissionsRationaleStateChanged = {},
            onRequestPermissionsStateChanged = {},
            onSettingsScreenPaused = {},
        )
    }
}

private class SettingsCameraUploadsViewParameterProvider
    : PreviewParameterProvider<SettingsCameraUploadsState> {
    override val values: Sequence<SettingsCameraUploadsState>
        get() = sequenceOf(
            // Initial Configuration - Camera Uploads Disabled
            SettingsCameraUploadsState(),
            // Camera Uploads Enabled
            SettingsCameraUploadsState(isCameraUploadsEnabled = true)
        )

}

/**
 * Test Tags for Settings Camera Uploads View
 */
internal const val SETTINGS_CAMERA_UPLOADS_TOOLBAR = "settings_camera_uploads_view:mega_app_bar"