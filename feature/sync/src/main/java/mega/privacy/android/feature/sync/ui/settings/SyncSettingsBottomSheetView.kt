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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState as rememberMaterial3ModalBottomSheetState
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
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.AndroidThemeForPreviews
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
    sheetElevation: Dp,
    shouldShowBottomSheet: Boolean,
    modalSheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    onOptionSelected: () -> Unit
) {
    SyncSettingsBottomSheetContent(
        modalSheetState = modalSheetState,
        sheetElevation = sheetElevation,
        shouldShowBottomSheet = shouldShowBottomSheet,
        onOptionSelected = onOptionSelected,
    )
}

/**
 * Sync Settings Bottom Sheet View for Material3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsBottomSheetViewM3(
    shouldShowBottomSheet: Boolean,
    sheetState: SheetState = rememberMaterial3ModalBottomSheetState(
        skipPartiallyExpanded = true
    ),
    onOptionSelected: () -> Unit
) {
    SyncSettingsBottomSheetContentM3(
        sheetState = sheetState,
        shouldShowBottomSheet = shouldShowBottomSheet,
        onOptionSelected = onOptionSelected,
    )
}

@Composable
private fun SyncSettingsDialogs(
    uiState: SettingsSyncUiState,
    viewModel: SettingsSyncViewModel,
    showSyncConnectionTypeDialog: Boolean,
    showSyncPowerOptionsDialog: Boolean,
    onSyncConnectionTypeDialogDismiss: () -> Unit,
    onSyncPowerOptionsDialogDismiss: () -> Unit,
) {
    if (showSyncConnectionTypeDialog) {
        SyncConnectionTypesDialog(
            onDismiss = onSyncConnectionTypeDialogDismiss,
            selectedOption = uiState.syncConnectionType,
            onSyncNetworkOptionsClicked = {
                onSyncConnectionTypeDialogDismiss()
                viewModel.handleAction(SettingsSyncAction.SyncConnectionTypeSelected(it))
            },
        )
    }
    if (showSyncPowerOptionsDialog) {
        SyncPowerOptionsDialog(
            onDismiss = onSyncPowerOptionsDialogDismiss,
            selectedOption = uiState.syncPowerOption,
            onSyncPowerOptionsClicked = {
                onSyncPowerOptionsDialogDismiss()
                viewModel.handleAction(SettingsSyncAction.SyncPowerOptionSelected(it))
            },
        )
    }
}

@Composable
internal fun SyncSettingsBottomSheetContent(
    modalSheetState: ModalBottomSheetState,
    shouldShowBottomSheet: Boolean,
    viewModel: SettingsSyncViewModel = hiltViewModel(),
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    onOptionSelected: () -> Unit
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
                SyncSettingsSheetBody(
                    uiState = uiState,
                    onOptionSelected = onOptionSelected,
                    onClearIssuesClicked = {
                        viewModel.handleAction(SettingsSyncAction.ClearSyncResolvedIssuesClicked)
                        Analytics.tracker.trackEvent(AndroidSyncClearResolvedIssuesEvent)
                    },
                    onConnectionTypeClicked = { showSyncConnectionTypeDialog = true },
                    onPowerOptionsClicked = { showSyncPowerOptionsDialog = true }
                )
            },
        )
    }
    SyncSettingsDialogs(
        uiState = uiState,
        viewModel = viewModel,
        showSyncConnectionTypeDialog = showSyncConnectionTypeDialog,
        showSyncPowerOptionsDialog = showSyncPowerOptionsDialog,
        onSyncConnectionTypeDialogDismiss = { showSyncConnectionTypeDialog = false },
        onSyncPowerOptionsDialogDismiss = { showSyncPowerOptionsDialog = false }
    )
}

/**
 * Sync Settings Bottom Sheet content for Material3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncSettingsBottomSheetContentM3(
    sheetState: SheetState,
    shouldShowBottomSheet: Boolean,
    viewModel: SettingsSyncViewModel = hiltViewModel(),
    onOptionSelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSyncConnectionTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncPowerOptionsDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowBottomSheet) {
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            onDismissRequest = {
                onOptionSelected()
            },
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            },
            sheetState = sheetState,
        ) {
            SyncSettingsSheetBody(
                topPadding = 0.dp,
                uiState = uiState,
                onOptionSelected = onOptionSelected,
                onClearIssuesClicked = {
                    viewModel.handleAction(SettingsSyncAction.ClearSyncResolvedIssuesClicked)
                    Analytics.tracker.trackEvent(AndroidSyncClearResolvedIssuesEvent)
                },
                onConnectionTypeClicked = { showSyncConnectionTypeDialog = true },
                onPowerOptionsClicked = { showSyncPowerOptionsDialog = true },
            )
        }
    }
    SyncSettingsDialogs(
        uiState = uiState,
        viewModel = viewModel,
        showSyncConnectionTypeDialog = showSyncConnectionTypeDialog,
        showSyncPowerOptionsDialog = showSyncPowerOptionsDialog,
        onSyncConnectionTypeDialogDismiss = { showSyncConnectionTypeDialog = false },
        onSyncPowerOptionsDialogDismiss = { showSyncPowerOptionsDialog = false }
    )
}

@Composable
private fun SyncSettingsSheetBody(
    uiState: SettingsSyncUiState,
    onOptionSelected: () -> Unit,
    onClearIssuesClicked: () -> Unit,
    onConnectionTypeClicked: () -> Unit,
    onPowerOptionsClicked: () -> Unit,
    topPadding: Dp = 16.dp,
) {
    Column {
        Spacer(modifier = Modifier.height(topPadding))
        if (uiState.shouldShowCleanSolvedIssueMenuItem) {
            MegaText(
                text = stringResource(syncR.string.sync_menu_clear_issues),
                textColor = TextColor.Primary,
                modifier = Modifier
                    .clickable {
                        onOptionSelected()
                        onClearIssuesClicked()
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
                    onConnectionTypeClicked()
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
                    onPowerOptionsClicked()
                }
                .fillMaxWidth()
                .padding(all = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun SyncPowerOptionsDialogPreviewM3() {
    AndroidThemeForPreviews {
        val sheetState = rememberMaterial3ModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        SyncSettingsBottomSheetContentM3(
            sheetState = sheetState,
            shouldShowBottomSheet = true,
            onOptionSelected = {}
        )
    }
}
