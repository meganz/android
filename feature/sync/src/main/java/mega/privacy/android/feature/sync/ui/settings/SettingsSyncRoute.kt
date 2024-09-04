package mega.privacy.android.feature.sync.ui.settings

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.feature.sync.ui.views.ClearSyncDebrisDialog
import mega.privacy.android.feature.sync.ui.views.SyncFrequencyDialog
import mega.privacy.android.feature.sync.ui.views.SyncOptionsDialog
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.SyncOptionSelected
import mega.privacy.mobile.analytics.event.SyncOptionSelectedEvent

@Composable
internal fun SettingsSyncRoute(
    viewModel: SettingsSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingSyncScreen(
        uiState = uiState,
        syncDebrisCleared = {
            viewModel.handleAction(SettingsSyncAction.ClearDebrisClicked)
        },
        syncOptionSelected = { selectedOption ->
            viewModel.handleAction(SettingsSyncAction.SyncOptionSelected(selectedOption))
        },
        syncFrequencySelected = { selectedFrequency ->
            viewModel.handleAction(SettingsSyncAction.SyncFrequencySelected(selectedFrequency))
        },
    )
}

@Composable
private fun SettingSyncScreen(
    uiState: SettingsSyncUiState,
    syncDebrisCleared: () -> Unit,
    syncOptionSelected: (SyncOption) -> Unit,
    syncFrequencySelected: (SyncFrequency) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var showSyncOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showClearSyncDebrisDialog by rememberSaveable { mutableStateOf(false) }
    var showSyncFrequencyDialog by rememberSaveable { mutableStateOf(false) }

    MegaScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(SETTINGS_SYNC_TOOLBAR),
                title = stringResource(R.string.settings_section_sync),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
                elevation = 0.dp
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
            ) {
                SyncOptionView(
                    syncOption = uiState.syncOption,
                    syncOptionsClicked = {
                        showSyncOptionsDialog = true
                    },
                )
                SyncDebrisView(
                    size = uiState.syncDebrisSizeInBytes ?: 0,
                    clearDebrisClicked = {
                        showClearSyncDebrisDialog = true
                    }
                )
                if (uiState.showSyncFrequency) {
                    SyncFrequencyView(
                        currentSyncFrequency = uiState.syncFrequency,
                        syncFrequencyClicked = {
                            showSyncFrequencyDialog = true
                        },
                    )
                }
            }
        }
    )
    if (showSyncOptionsDialog) {
        SyncOptionsDialog(
            onDismiss = {
                showSyncOptionsDialog = false
            },
            selectedOption = uiState.syncOption,
            onSyncOptionsClicked = { selectedSyncOption ->
                when (selectedSyncOption) {
                    SyncOption.WI_FI_OR_MOBILE_DATA -> {
                        Analytics.tracker.trackEvent(
                            SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiAndMobileSelected)
                        )
                    }

                    SyncOption.WI_FI_ONLY -> {
                        Analytics.tracker.trackEvent(
                            SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiOnlySelected)
                        )
                    }
                }

                syncOptionSelected(selectedSyncOption)
                showSyncOptionsDialog = false
            },
        )
    }
    if (showClearSyncDebrisDialog) {
        ClearSyncDebrisDialog(
            onDismiss = {
                showClearSyncDebrisDialog = false
            },
            onConfirm = {
                syncDebrisCleared()
                showClearSyncDebrisDialog = false
            }
        )
    }
    if (showSyncFrequencyDialog) {
        SyncFrequencyDialog(
            onDismiss = {
                showSyncFrequencyDialog = false
            },
            selectedSyncFrequency = uiState.syncFrequency,
            onSyncFrequencyClicked = { selectedSyncFrequency ->
                syncFrequencySelected(selectedSyncFrequency)
                showSyncFrequencyDialog = false
            },
        )
    }
    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                context.resources.getString(
                    message
                )
            )
        }
    }
}


private const val SETTINGS_SYNC_TOOLBAR = "SETTINGS_SYNC_TOOLBAR"
