package mega.privacy.android.app.camera.setting

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun CameraSettingBottomSheet(
    modifier: Modifier = Modifier,
    state: CameraSettingUiState = CameraSettingUiState(),
    onEnableGeoTagging: (Boolean) -> Unit = {},
    showPermissionDeniedSnackbar: () -> Unit = {},
) {
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onEnableGeoTagging(true)
            } else {
                showPermissionDeniedSnackbar()
            }
        }
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        MegaText(
            text = stringResource(sharedR.string.camera_settings_title),
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textColor = TextColor.Primary
        )

        GenericTwoLineListItem(
            title = stringResource(sharedR.string.camera_settings_save_location_title),
            subtitle = stringResource(sharedR.string.camera_settings_save_location_description),
            trailingIcons = {
                MegaSwitch(
                    modifier = Modifier.testTag(SAVE_LOCATION_SWITCH_TEST_TAG),
                    checked = state.isGeoTaggingEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            onEnableGeoTagging(false)
                        }
                    }
                )
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraSettingBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CameraSettingBottomSheet()
    }
}

internal const val SAVE_LOCATION_SWITCH_TEST_TAG = "camera_setting:save_location_switch"