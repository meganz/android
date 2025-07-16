package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.R as syncR
import mega.privacy.android.feature.sync.ui.views.SyncConnectionTypesDialog
import mega.privacy.android.feature.sync.ui.views.SyncPowerOptionsDialog
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AndroidSyncClearResolvedIssuesEvent

@Composable
fun SyncSettingsBottomSheetView(
    modalSheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    sheetElevation: Dp,
    shouldShowBottomSheet: Boolean,
    onOptionSelected: () -> Unit = { }, // Callback for when an option is selected, if needed
) {
    SyncSettingsBottomSheetContent(
        modalSheetState = modalSheetState,
        sheetElevation = sheetElevation,
        shouldShowBottomSheet = shouldShowBottomSheet,
        onOptionSelected = onOptionSelected,
    )
}

@Composable
internal fun SyncSettingsBottomSheetContent(
    viewModel: SettingsSyncViewModel = hiltViewModel(),
    modalSheetState: ModalBottomSheetState,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    shouldShowBottomSheet: Boolean,
    onOptionSelected: () -> Unit = { }, // Callback for when an option is selected, if needed
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSyncConnectionTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncPowerOptionsDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowBottomSheet) {
        BottomSheet(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            },
            modalSheetState = modalSheetState,
            sheetElevation = sheetElevation,
            bottomInsetPadding = true,
            expandedRoundedCorners = true,
            sheetBody = {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.shouldShowCleanSolvedIssueMenuItem) {
                        MegaText(
                            text = stringResource(syncR.string.sync_menu_clear_issues),
                            textColor = TextColor.Primary,
                            modifier = Modifier
                                .clickable {
                                    onOptionSelected()
                                    viewModel.handleAction(SettingsSyncAction.ClearSyncResolvedIssuesClicked)
                                    Analytics.tracker.trackEvent(AndroidSyncClearResolvedIssuesEvent)
                                }
                                .fillMaxWidth()
                                .padding(all = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    MegaText(
                        text = stringResource(sharedR.string.settings_sync_connection_type_title),
                        textColor = TextColor.Primary,
                        modifier = Modifier
                            .clickable {
                                onOptionSelected()
                                showSyncConnectionTypeDialog = true
                            }
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MegaText(
                        text = stringResource(sharedR.string.settings_sync_power_settings_title),
                        textColor = TextColor.Primary,
                        modifier = Modifier
                            .clickable {
                                onOptionSelected()
                                showSyncPowerOptionsDialog = true
                            }
                            .fillMaxWidth()
                            .padding(all = 16.dp)

                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
        )
    }
    if (showSyncConnectionTypeDialog) {
        SyncConnectionTypesDialog(
            onDismiss = {
                showSyncConnectionTypeDialog = false
            },
            selectedOption = uiState.syncConnectionType,
            onSyncNetworkOptionsClicked = {
                showSyncConnectionTypeDialog = false
                viewModel.handleAction(SettingsSyncAction.SyncConnectionTypeSelected(it))
            },
        )
    }
    if (showSyncPowerOptionsDialog) {
        SyncPowerOptionsDialog(
            onDismiss = {
                showSyncPowerOptionsDialog = false
            },
            selectedOption = uiState.syncPowerOption,
            onSyncPowerOptionsClicked = {
                showSyncPowerOptionsDialog = false
                viewModel.handleAction(SettingsSyncAction.SyncPowerOptionSelected(it))
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncPowerOptionsDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.HalfExpanded,
            skipHalfExpanded = false,
        )
        SyncSettingsBottomSheetContent(
            modalSheetState = modalSheetState,
            shouldShowBottomSheet = true,
            onOptionSelected = {}
        )
    }
}
