package mega.privacy.android.feature.sync.ui.synclist

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import androidx.lifecycle.Lifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.banner.InlineWarningBanner
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.SyncMonitorState
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SOLVED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.STALLED_ISSUES
import mega.privacy.android.feature.sync.ui.synclist.SyncChip.SYNC_FOLDERS
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.folders.TEST_TAG_SYNC_LIST_SCREEN_FAB
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.views.SyncNotificationWarningBanner
import mega.privacy.android.feature.sync.ui.views.SyncPermissionWarningBanner
import mega.privacy.android.feature.sync.ui.views.SyncStorageQuotaExceedWarning
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaMultiFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonItem
import mega.privacy.android.shared.original.core.ui.controls.buttons.MultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.controls.buttons.rememberMultiFloatingActionButtonState
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.utils.ComposableLifecycle
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.mobile.analytics.event.AndroidBackupFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncFABButtonEvent
import mega.privacy.mobile.analytics.event.AndroidSyncMultiFABButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListFoldersButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListIssuesButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncListSolvedIssuesButtonPressedEvent

/**
 * Standalone sync list screen. It owns the screen shell — top app bar, FAB and the single
 * snackbar host — so it must not be used where an ancestor already provides one.
 */
@Composable
internal fun SyncListScreen(
    syncFoldersUiState: SyncFoldersUiState,
    syncStalledIssuesState: SyncStalledIssuesState,
    syncSolvedIssuesState: SyncSolvedIssuesState,
    syncNotificationState: SyncMonitorState,
    stalledIssuesCount: Int,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    actions: List<MenuAction>,
    onActionPressed: (MenuAction) -> Unit,
    onOpenUpgradeAccountClicked: () -> Unit,
    onDismissNotification: () -> Unit,
    onSyncRefresh: () -> Unit,
    title: String,
    chipContent: @Composable (chip: SyncChip, onIssuesInfoClicked: () -> Unit) -> Unit,
    selectedChip: SyncChip = SYNC_FOLDERS,
    onFabExpanded: (Boolean) -> Unit = {},
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var isWarningBannerDisplayed by rememberSaveable { mutableStateOf(false) }
    ComposableLifecycle { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            isWarningBannerDisplayed =
                syncPermissionsManager.isDisableBatteryOptimizationGranted().not()
        }
    }

    val multiFabState = rememberMultiFloatingActionButtonState()
    FabExpandedEffect(multiFabState, onFabExpanded)

    MegaScaffold(
        topBar = {
            MegaTopAppBar(
                title = title.ifEmpty { stringResource(R.string.sync_toolbar_title) },
                navigationType = AppBarNavigationType.Back {
                    onBackPressedDispatcher?.onBackPressed()
                },
                actions = actions.map { action ->
                    MenuActionWithClick(action) { onActionPressed(action) }
                },
                // M3 draws the separator from scroll state rather than a fixed elevation.
                drawBottomLineOnScrolledContent = isWarningBannerDisplayed ||
                        syncFoldersUiState.isWarningBannerDisplayed,
            )
        },
        floatingActionButton = {
            SyncListFab(
                modifier = Modifier.navigationBarsPadding(),
                syncFoldersUiState = syncFoldersUiState,
                multiFabState = multiFabState,
                onSyncFolderClicked = onSyncFolderClicked,
                onBackupFolderClicked = onBackupFolderClicked,
                onFabExpanded = onFabExpanded,
            )
        },
        content = { paddingValues ->
            SyncListContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .collapseFabOnTap(multiFabState),
                syncFoldersUiState = syncFoldersUiState,
                syncStalledIssuesState = syncStalledIssuesState,
                syncSolvedIssuesState = syncSolvedIssuesState,
                syncNotificationState = syncNotificationState,
                stalledIssuesCount = stalledIssuesCount,
                syncPermissionsManager = syncPermissionsManager,
                onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
                onDismissNotification = onDismissNotification,
                onSyncRefresh = onSyncRefresh,
                selectedChip = selectedChip,
                chipContent = chipContent,
            )
        },
    )
}

/**
 * Sync list as a tab of another screen. The host screen owns the scaffold, top app bar,
 * snackbar host and FAB; adding a second scaffold here would render every snackbar twice, and
 * a FAB outside the host scaffold would be covered by those snackbars. The host places the FAB
 * with [SyncListTabFab] and shares [fabState] so a tap on this content still collapses it.
 */
@Composable
internal fun SyncListTabContent(
    syncFoldersUiState: SyncFoldersUiState,
    syncStalledIssuesState: SyncStalledIssuesState,
    syncSolvedIssuesState: SyncSolvedIssuesState,
    syncNotificationState: SyncMonitorState,
    stalledIssuesCount: Int,
    syncPermissionsManager: SyncPermissionsManager,
    onOpenUpgradeAccountClicked: () -> Unit,
    onDismissNotification: () -> Unit,
    onSyncRefresh: () -> Unit,
    chipContent: @Composable (chip: SyncChip, onIssuesInfoClicked: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    fabState: SyncListFabState = rememberSyncListFabState(),
    selectedChip: SyncChip = SYNC_FOLDERS,
) {
    SyncListContent(
        modifier = modifier
            .fillMaxSize()
            .collapseFabOnTap(fabState.multiFabState),
        syncFoldersUiState = syncFoldersUiState,
        syncStalledIssuesState = syncStalledIssuesState,
        syncSolvedIssuesState = syncSolvedIssuesState,
        syncNotificationState = syncNotificationState,
        stalledIssuesCount = stalledIssuesCount,
        syncPermissionsManager = syncPermissionsManager,
        onOpenUpgradeAccountClicked = onOpenUpgradeAccountClicked,
        onDismissNotification = onDismissNotification,
        onSyncRefresh = onSyncRefresh,
        selectedChip = selectedChip,
        chipContent = chipContent,
    )
}

@Composable
internal fun SyncListFab(
    syncFoldersUiState: SyncFoldersUiState,
    multiFabState: MutableState<MultiFloatingActionButtonState>,
    onSyncFolderClicked: () -> Unit,
    onBackupFolderClicked: () -> Unit,
    onFabExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSyncs = syncFoldersUiState.syncUiItems.isNotEmpty() || syncFoldersUiState.isLoading
    if (hasSyncs && syncFoldersUiState.isStorageOverQuota.not()) {
        // MegaMultiFloatingActionButton applies its modifier to the button itself, not to the
        // root of the expandable column, so positioning has to be applied to a wrapper.
        Box(modifier = modifier) {
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = stringResource(id = R.string.sync_toolbar_title),
                        onClicked = {
                            Analytics.tracker.trackEvent(AndroidSyncFABButtonEvent)
                            onSyncFolderClicked()
                            multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                        },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = stringResource(id = sharedR.string.sync_add_new_backup_toolbar_title),
                        onClicked = {
                            Analytics.tracker.trackEvent(AndroidBackupFABButtonPressedEvent)
                            onBackupFolderClicked()
                            multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
                        },
                    ),
                ),
                modifier = Modifier.testTag(TEST_TAG_SYNC_LIST_SCREEN_FAB),
                multiFabState = multiFabState,
                onStateChanged = { state ->
                    if (state == MultiFloatingActionButtonState.EXPANDED) {
                        Analytics.tracker.trackEvent(AndroidSyncMultiFABButtonPressedEvent)
                    }
                    onFabExpanded(state == MultiFloatingActionButtonState.EXPANDED)
                    multiFabState.value = state
                },
                isCircular = false,
            )
        }
    }
}

@Composable
internal fun FabExpandedEffect(
    multiFabState: MutableState<MultiFloatingActionButtonState>,
    onFabExpanded: (Boolean) -> Unit,
) {
    DisposableEffect(multiFabState.value) {
        onFabExpanded(multiFabState.value == MultiFloatingActionButtonState.EXPANDED)
        onDispose { }
    }
}

private fun Modifier.collapseFabOnTap(
    multiFabState: MutableState<MultiFloatingActionButtonState>,
) = conditional(multiFabState.value == MultiFloatingActionButtonState.EXPANDED) {
    clickable(
        interactionSource = null,
        indication = null,
    ) {
        multiFabState.value = MultiFloatingActionButtonState.COLLAPSED
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun SyncListContent(
    modifier: Modifier,
    syncFoldersUiState: SyncFoldersUiState,
    syncStalledIssuesState: SyncStalledIssuesState,
    syncSolvedIssuesState: SyncSolvedIssuesState,
    syncNotificationState: SyncMonitorState,
    stalledIssuesCount: Int,
    syncPermissionsManager: SyncPermissionsManager,
    onOpenUpgradeAccountClicked: () -> Unit,
    onDismissNotification: () -> Unit,
    onSyncRefresh: () -> Unit,
    chipContent: @Composable (chip: SyncChip, onIssuesInfoClicked: () -> Unit) -> Unit,
    selectedChip: SyncChip = SYNC_FOLDERS,
) {
    var checkedChip by rememberSaveable { mutableStateOf(selectedChip) }

    val isSyncNotEmpty = syncFoldersUiState.syncUiItems.isNotEmpty()

    Column(modifier) {
        if (syncFoldersUiState.isStorageOverQuota) {
            SyncStorageQuotaExceedWarning(onUpgradeClick = onOpenUpgradeAccountClicked)
        } else {
            SyncPermissionWarningBanner(
                syncPermissionsManager = syncPermissionsManager,
                isDisableBatteryOptimizationEnabled = syncFoldersUiState.isDisableBatteryOptimizationEnabled
            )
            SyncNotificationWarningBanner(
                syncNotificationState,
                onDismissNotification = onDismissNotification,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (syncFoldersUiState.syncUiItems.isNotEmpty() && syncFoldersUiState.isLowBatteryLevel) {
                InlineWarningBanner(
                    body = stringResource(id = sharedR.string.general_message_sync_paused_low_battery_level),
                    showCancelButton = false,
                )
            }
        }
        if (checkedChip != SYNC_FOLDERS || syncStalledIssuesState.stalledIssues.isNotEmpty() || syncSolvedIssuesState.solvedIssues.isNotEmpty()) {
            if (syncFoldersUiState.syncUiItems.isNotEmpty()) {
                // deferred loading of HeaderChips to avoid showing chips first before sync items are loaded
                HeaderChips(
                    selectedChip = checkedChip,
                    stalledIssuesCount = stalledIssuesCount,
                    onChipSelected = { checkedChip = it })
            }
        }

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isSyncNotEmpty && syncFoldersUiState.isRefreshing,
            onRefresh = {
                if (isSyncNotEmpty) onSyncRefresh()
            },
        ) {
            chipContent(checkedChip) { checkedChip = STALLED_ISSUES }
        }
    }
}

@Composable
internal fun HeaderChips(
    selectedChip: SyncChip,
    stalledIssuesCount: Int,
    onChipSelected: (SyncChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaChip(
            selected = selectedChip == SYNC_FOLDERS,
            text = stringResource(id = R.string.sync_folders),
            modifier = Modifier.testTag(SYNC_FOLDERS_CHIP_TEST_TAG),
            onClick = {
                Analytics.tracker.trackEvent(SyncListFoldersButtonPressedEvent)
                onChipSelected(SYNC_FOLDERS)
            },
        )
        MegaChip(
            selected = selectedChip == STALLED_ISSUES,
            text = if (stalledIssuesCount > 0) {
                stringResource(R.string.sync_stalled_issues, stalledIssuesCount)
            } else {
                stringResource(id = R.string.sync_stalled_issue_zero)
            },
            modifier = Modifier.testTag(STALLED_ISSUES_CHIP_TEST_TAG),
            onClick = {
                Analytics.tracker.trackEvent(SyncListIssuesButtonPressedEvent)
                onChipSelected(STALLED_ISSUES)
            },
        )
        MegaChip(
            selected = selectedChip == SOLVED_ISSUES,
            text = stringResource(id = sharedR.string.device_center_sync_solved_issues_chip_text),
            modifier = Modifier.testTag(SOLVED_ISSUES_CHIP_TEST_TAG),
            onClick = {
                Analytics.tracker.trackEvent(SyncListSolvedIssuesButtonPressedEvent)
                onChipSelected(SOLVED_ISSUES)
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncListTabContentPreview() {
    AndroidThemeForPreviews {
        SyncListTabContent(
            syncFoldersUiState = SyncFoldersUiState(syncUiItems = persistentListOf()),
            syncStalledIssuesState = SyncStalledIssuesState(stalledIssues = emptyList()),
            syncSolvedIssuesState = SyncSolvedIssuesState(),
            syncNotificationState = SyncMonitorState(),
            stalledIssuesCount = 0,
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
            onOpenUpgradeAccountClicked = {},
            onDismissNotification = {},
            onSyncRefresh = {},
            chipContent = { _, _ -> },
        )
    }
}

/**
 * Sync Folders Chip test tag
 */
internal const val SYNC_FOLDERS_CHIP_TEST_TAG = "sync_list:folders_chip"

/**
 * Stalled Issues Chip test tag
 */
internal const val STALLED_ISSUES_CHIP_TEST_TAG = "sync_list:stalled_issues_chip"

/**
 * Solved Issues Chip test tag
 */
internal const val SOLVED_ISSUES_CHIP_TEST_TAG = "sync_list:solved_issues_chip"

internal const val BOTTOM_PADDING = 72
