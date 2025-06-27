package mega.privacy.android.app.presentation.transfers.view

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.TopAppBarAction
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackbarHostStateWrapper
import mega.privacy.android.app.presentation.snackbar.showAutoDurationSnackbar
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.view.active.ActiveTransfersView
import mega.privacy.android.app.presentation.transfers.view.completed.CompletedTransfersView
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelAllTransfersDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelTransfersConfirmationDialog
import mega.privacy.android.app.presentation.transfers.view.failed.FailedTransfersView
import mega.privacy.android.app.presentation.transfers.view.sheet.ActiveTransfersActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.CompletedTransfersActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.FailedTransfersActionsBottomSheet
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ActiveTransferPriorityChangedEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersCancelSelectedMenuItemEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersGlobalPauseMenuItemEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersGlobalPlayMenuItemEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersMoreOptionsMenuItemEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersSelectAllMenuItemEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersTabEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersClearSelectedMenuItemEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersMoreOptionsMenuItemEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersSelectAllMenuItemEvent
import mega.privacy.mobile.analytics.event.CompletedTransfersTabEvent
import mega.privacy.mobile.analytics.event.FailedTransfersClearSelectedMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersMoreOptionsMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersRetrySelectedMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersSelectAllMenuItemEvent
import mega.privacy.mobile.analytics.event.FailedTransfersTabEvent
import mega.privacy.mobile.analytics.event.TransfersSectionScreenEvent

@Composable
internal fun TransfersView(
    onBackPress: () -> Unit,
    onNavigateToStorageSettings: () -> Unit,
    onNavigateToUpgradeAccount: () -> Unit,
    uiState: TransfersUiState,
    onTabSelected: (Int) -> Unit,
    onPlayPauseTransfer: (Int) -> Unit,
    onResumeTransfers: () -> Unit,
    onPauseTransfers: () -> Unit,
    onRetryFailedTransfers: () -> Unit,
    onCancelAllFailedTransfers: () -> Unit,
    onClearAllCompletedTransfers: () -> Unit,
    onClearAllFailedTransfers: () -> Unit,
    onActiveTransfersReorderPreview: suspend (from: Int, to: Int) -> Unit,
    onActiveTransfersReorderConfirmed: (InProgressTransfer) -> Unit,
    onConsumeStartEvent: () -> Unit,
    onSelectActiveTransfers: () -> Unit,
    onSelectCompletedTransfers: () -> Unit,
    onSelectFailedTransfers: () -> Unit,
    onSelectTransfersClose: () -> Unit,
    onActiveTransferSelected: (InProgressTransfer) -> Unit,
    onCompletedTransferSelected: (CompletedTransfer) -> Unit,
    onFailedTransferSelected: (CompletedTransfer) -> Unit,
    onCancelSelectedActiveTransfers: () -> Unit,
    onClearSelectedCompletedTransfers: () -> Unit,
    onClearSelectedFailedTransfers: () -> Unit,
    onRetrySelectedFailedTransfers: () -> Unit,
    onSelectAllActiveTransfers: () -> Unit,
    onSelectAllCompletedTransfers: () -> Unit,
    onSelectAllFailedTransfers: () -> Unit,
    onRetryTransfer: (CompletedTransfer) -> Unit,
    onConsumeQuotaWarning: () -> Unit,
) = with(uiState) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showActiveTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showCompletedTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showFailedTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showCancelAllTransfersDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmCancelTransfersDialog by rememberSaveable { mutableStateOf(false) }

    // Track screen view event
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(TransfersSectionScreenEvent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_TRANSFERS_VIEW),
        topBar = {
            val snackbarHostState = LocalSnackBarHostState.current?.let {
                SnackbarHostStateWrapper(it)
            }

            if (!isInSelectTransfersMode) {
                TransfersTopBar(onBackPress, getTransferActions(uiState)) { action ->
                    when (action) {
                        TransferMenuAction.Pause -> {
                            Analytics.tracker.trackEvent(ActiveTransfersGlobalPauseMenuItemEvent)
                            onPauseTransfers()

                            coroutineScope.launch {
                                val result = snackbarHostState?.showAutoDurationSnackbar(
                                    context.getString(sharedR.string.transfers_all_transfers_paused_warning),
                                    context.getString(sharedR.string.transfers_resume_all_button)
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    onResumeTransfers()
                                }
                            }
                        }

                        TransferMenuAction.Resume -> {
                            Analytics.tracker.trackEvent(ActiveTransfersGlobalPlayMenuItemEvent)
                            onResumeTransfers()
                        }

                        TransferMenuAction.More -> {
                            when (selectedTab) {
                                ACTIVE_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        ActiveTransfersMoreOptionsMenuItemEvent
                                    )
                                    showActiveTransfersModal = true
                                }

                                COMPLETED_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        CompletedTransfersMoreOptionsMenuItemEvent
                                    )
                                    showCompletedTransfersModal = true
                                }

                                FAILED_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        FailedTransfersMoreOptionsMenuItemEvent
                                    )
                                    showFailedTransfersModal = true
                                }
                            }
                        }
                    }
                }
            } else {
                SelectActiveTransfersTopBar(
                    onClose = onSelectTransfersClose,
                    selectedAmount = selectedTransfersAmount,
                    actions = getTransferActions(uiState)
                ) { action ->
                    when (action) {
                        TransferMenuAction.SelectAll -> {
                            when (selectedTab) {
                                ACTIVE_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        ActiveTransfersSelectAllMenuItemEvent
                                    )
                                    onSelectAllActiveTransfers()
                                }

                                COMPLETED_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        CompletedTransfersSelectAllMenuItemEvent
                                    )
                                    onSelectAllCompletedTransfers()
                                }

                                FAILED_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        FailedTransfersSelectAllMenuItemEvent
                                    )
                                    onSelectAllFailedTransfers()
                                }
                            }
                        }

                        TransferMenuAction.CancelSelected, TransferMenuAction.ClearSelected -> {
                            when (selectedTab) {
                                ACTIVE_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        ActiveTransfersCancelSelectedMenuItemEvent
                                    )
                                    showConfirmCancelTransfersDialog = true
                                }

                                COMPLETED_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        CompletedTransfersClearSelectedMenuItemEvent
                                    )
                                    onClearSelectedCompletedTransfers()
                                }

                                FAILED_TAB_INDEX -> {
                                    Analytics.tracker.trackEvent(
                                        FailedTransfersClearSelectedMenuItemEvent
                                    )
                                    onClearSelectedFailedTransfers()
                                }
                            }
                        }

                        TransferMenuAction.RetrySelected -> {
                            Analytics.tracker.trackEvent(FailedTransfersRetrySelectedMenuItemEvent)
                            onRetrySelectedFailedTransfers()
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        val noTransfers =
            activeTransfers.isEmpty() && completedTransfers.isEmpty() && failedTransfers.isEmpty()

        if (noTransfers) {
            EmptyTransfersView(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(TEST_TAG_EMPTY_TRANSFERS_VIEW),
                emptyStringId = sharedR.string.transfers_no_transfers_empty_text,
            )
        } else {
            MegaScrollableTabRow(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                beyondViewportPageCount = 1,
                hideTabs = isInSelectTransfersMode,
                pagerScrollEnabled = false,
                cells = {
                    addTextTabWithLazyListState(
                        tabItem = TabItems(stringResource(id = sharedR.string.transfers_section_tab_title_active_transfers)),
                    ) { _, listState, modifier ->
                        ActiveTransfersView(
                            activeTransfers = activeTransfers,
                            isTransferOverQuota = isTransferOverQuota,
                            isStorageOverQuota = isStorageOverQuota,
                            quotaWarning = quotaWarning,
                            areTransfersPaused = areTransfersPaused,
                            onPlayPauseClicked = { tag ->
                                onPlayPauseTransfer(tag)
                            },
                            onReorderPreview = onActiveTransfersReorderPreview,
                            onReorderConfirmed = {
                                Analytics.tracker.trackEvent(ActiveTransferPriorityChangedEvent)
                                onActiveTransfersReorderConfirmed(it)
                            },
                            onActiveTransferSelected = onActiveTransferSelected,
                            selectedActiveTransfersIds = selectedActiveTransfersIds,
                            onUpgradeClick = onNavigateToUpgradeAccount,
                            onConsumeQuotaWarning = onConsumeQuotaWarning,
                            lazyListState = listState,
                            modifier = modifier,
                        )
                    }
                    addTextTabWithLazyListState(
                        tabItem = TabItems(stringResource(id = R.string.title_tab_completed_transfers)),
                    ) { _, listState, modifier ->
                        CompletedTransfersView(
                            completedTransfers = completedTransfers,
                            lazyListState = listState,
                            onCompletedTransferSelected = onCompletedTransferSelected,
                            selectedCompletedTransfersIds = selectedCompletedTransfersIds,
                            modifier = modifier,
                        )
                    }
                    addTextTabWithLazyListState(
                        tabItem = TabItems(stringResource(id = sharedR.string.transfers_section_tab_title_failed_transfers)),
                    ) { _, listState, modifier ->
                        FailedTransfersView(
                            failedTransfers = failedTransfers,
                            lazyListState = listState,
                            onFailedTransferSelected = onFailedTransferSelected,
                            selectedFailedTransfersIds = selectedFailedTransfersIds,
                            onRetryTransfer = onRetryTransfer,
                            modifier = modifier,
                        )
                    }
                },
                initialSelectedIndex = selectedTab,
                onTabSelected = {
                    when (it) {
                        ACTIVE_TAB_INDEX -> ActiveTransfersTabEvent
                        COMPLETED_TAB_INDEX -> CompletedTransfersTabEvent
                        FAILED_TAB_INDEX -> FailedTransfersTabEvent
                        else -> null
                    }?.let { tabSelectedEvent ->
                        Analytics.tracker.trackEvent(tabSelectedEvent)
                    }
                    onTabSelected(it)
                    true
                },
            )
        }

        if (showActiveTransfersModal) {
            ActiveTransfersActionsBottomSheet(
                onSelectTransfers = onSelectActiveTransfers,
                onCancelAllTransfers = {
                    showCancelAllTransfersDialog = true
                },
                onDismissSheet = { showActiveTransfersModal = false },
            )
        }

        if (showCompletedTransfersModal) {
            CompletedTransfersActionsBottomSheet(
                onClearAllTransfers = onClearAllCompletedTransfers,
                onSelectTransfers = onSelectCompletedTransfers,
                onDismissSheet = { showCompletedTransfersModal = false },
            )
        }

        if (showFailedTransfersModal) {
            FailedTransfersActionsBottomSheet(
                onRetryAllTransfers = onRetryFailedTransfers,
                onClearAllTransfers = onClearAllFailedTransfers,
                onSelectTransfers = onSelectFailedTransfers,
                onDismissSheet = { showFailedTransfersModal = false },
            )
        }

        if (showCancelAllTransfersDialog) {
            CancelAllTransfersDialog(
                onCancelAllTransfers = onCancelAllFailedTransfers,
                onDismiss = { showCancelAllTransfersDialog = false },
            )
        }

        if (showConfirmCancelTransfersDialog) {
            CancelTransfersConfirmationDialog(
                selectedAmount = selectedActiveTransfersIds?.size ?: 1,
                onCancelTransfers = {
                    onCancelSelectedActiveTransfers()
                },
                onDismiss = { showConfirmCancelTransfersDialog = false },
            )
        }
        StartTransferComponent(
            event = uiState.startEvent,
            onConsumeEvent = onConsumeStartEvent,
            navigateToStorageSettings = onNavigateToStorageSettings,
        )
    }
}

@Composable
internal fun EmptyTransfersView(
    @StringRes emptyStringId: Int,
    modifier: Modifier = Modifier,
) {
    EmptyStateView(
        modifier = modifier.fillMaxSize(),
        illustration = iconPackR.drawable.ic_arrow_up_down_glass,
        description = stringResource(id = emptyStringId),
        descriptionSpanStyles = mapOf(
            SpanIndicator('A') to SpanStyleWithAnnotation(
                megaSpanStyle = MegaSpanStyle.TextColorStyle(
                    SpanStyle().copy(fontWeight = AppTheme.typography.titleMedium.fontWeight),
                    TextColor.Primary
                ),
                annotation = null
            )
        ),
    )
}

@Composable
internal fun TransfersTopBar(
    onBackPress: () -> Unit,
    actions: List<TransferMenuAction>,
    onActionPressed: (TopAppBarAction) -> Unit,
) {
    MegaTopAppBar(
        navigationType = AppBarNavigationType.Back(onBackPress),
        title = stringResource(id = R.string.section_transfers),
        actions = actions,
        onActionPressed = onActionPressed,
    )
}

@Composable
internal fun SelectActiveTransfersTopBar(
    onClose: () -> Unit,
    selectedAmount: Int,
    actions: List<TransferMenuAction>,
    onActionPressed: (TopAppBarAction) -> Unit,
) {
    MegaTopAppBar(
        navigationType = AppBarNavigationType.Close(onClose),
        title = if (selectedAmount == 0) stringResource(R.string.title_select_transfers) else selectedAmount.toString(),
        actions = actions,
        onActionPressed = onActionPressed,
        drawBottomLineOnScrolledContent = true,
    )
}

@CombinedThemePreviews
@Composable
private fun TransfersViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TransfersView(
            onBackPress = {},
            onNavigateToStorageSettings = {},
            onNavigateToUpgradeAccount = {},
            uiState = TransfersUiState(),
            onTabSelected = {},
            onPlayPauseTransfer = {},
            onResumeTransfers = {},
            onPauseTransfers = {},
            onRetryFailedTransfers = {},
            onCancelAllFailedTransfers = {},
            onClearAllCompletedTransfers = {},
            onClearAllFailedTransfers = {},
            onActiveTransfersReorderPreview = { _, _ -> },
            onActiveTransfersReorderConfirmed = {},
            onConsumeStartEvent = {},
            onSelectActiveTransfers = {},
            onSelectCompletedTransfers = {},
            onSelectFailedTransfers = {},
            onSelectTransfersClose = {},
            onActiveTransferSelected = {},
            onCompletedTransferSelected = {},
            onFailedTransferSelected = {},
            onCancelSelectedActiveTransfers = {},
            onClearSelectedCompletedTransfers = {},
            onClearSelectedFailedTransfers = {},
            onRetrySelectedFailedTransfers = {},
            onSelectAllActiveTransfers = {},
            onSelectAllCompletedTransfers = {},
            onSelectAllFailedTransfers = {},
            onRetryTransfer = {},
            onConsumeQuotaWarning = {},
        )
    }
}

/**
 * Get the actions that should be shown given the ui state
 */
private fun getTransferActions(uiState: TransfersUiState) = with(uiState) {
    buildList<TransferMenuAction> {
        when {
            selectedTab == ACTIVE_TAB_INDEX && activeTransfers.isNotEmpty() -> {
                if (isInSelectTransfersMode) {
                    val isAtLeastOneActiveTransferSelected =
                        selectedActiveTransfersIds?.isNotEmpty() == true
                    if (isAtLeastOneActiveTransferSelected) {
                        add(TransferMenuAction.CancelSelected)
                    }
                    if (isAtLeastOneActiveTransferSelected && !areAllActiveTransfersSelected) {
                        add(TransferMenuAction.SelectAll)
                    }
                } else {
                    if (areTransfersPaused) {
                        add(TransferMenuAction.Resume)
                    } else {
                        add(TransferMenuAction.Pause)
                    }
                    add(TransferMenuAction.More)
                }
            }

            selectedTab == COMPLETED_TAB_INDEX && completedTransfers.isNotEmpty() -> {
                if (isInSelectTransfersMode) {
                    val isAtLeastOneTransferSelected =
                        selectedCompletedTransfersIds?.isNotEmpty() == true
                    if (isAtLeastOneTransferSelected) {
                        add(TransferMenuAction.ClearSelected)
                    }
                    if (isAtLeastOneTransferSelected && !areAllCompletedTransfersSelected) {
                        add(TransferMenuAction.SelectAll)
                    }
                } else {
                    add(TransferMenuAction.More)
                }
            }

            selectedTab == FAILED_TAB_INDEX && failedTransfers.isNotEmpty() -> {
                if (isInSelectTransfersMode) {
                    val isAtLeastOneTransferSelected =
                        selectedFailedTransfersIds?.isNotEmpty() == true
                    if (isAtLeastOneTransferSelected) {
                        add(TransferMenuAction.RetrySelected)
                        add(TransferMenuAction.ClearSelected)
                    }
                    if (isAtLeastOneTransferSelected && !areAllFailedTransfersSelected) {
                        add(TransferMenuAction.SelectAll)
                    }
                } else {
                    add(TransferMenuAction.More)
                }
            }
        }
    }
}

internal const val TEST_TAG_TRANSFERS_VIEW = "transfers_view"

internal const val TEST_TAG_EMPTY_TRANSFERS_VIEW = "$TEST_TAG_TRANSFERS_VIEW:empty"

/**
 * Tag for the active tab
 */
const val TEST_TAG_ACTIVE_TAB = "$TEST_TAG_TRANSFERS_VIEW:tab_active"

/**
 * Index for the active tab
 */
const val ACTIVE_TAB_INDEX = 0

/**
 * Tag for the completed tab
 */
const val TEST_TAG_COMPLETED_TAB = "$TEST_TAG_TRANSFERS_VIEW:tab_completed"

/**
 * Index for the completed tab
 */
const val COMPLETED_TAB_INDEX = 1

/**
 * Tag for the failed tab
 */
const val TEST_TAG_FAILED_TAB = "$TEST_TAG_TRANSFERS_VIEW:tab_failed"

/**
 * Index for the failed tab
 */
const val FAILED_TAB_INDEX = 2
