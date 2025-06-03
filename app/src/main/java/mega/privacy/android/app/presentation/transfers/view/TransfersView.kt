package mega.privacy.android.app.presentation.transfers.view

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.view.active.ActiveTransfersView
import mega.privacy.android.app.presentation.transfers.view.completed.CompletedTransfersView
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelAllTransfersDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelTransfersConfirmationDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.ClearAllTransfersDialog
import mega.privacy.android.app.presentation.transfers.view.failed.FailedTransfersView
import mega.privacy.android.app.presentation.transfers.view.sheet.ActiveTransfersActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.CompletedTransfersActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.FailedTransfersActionsBottomSheet
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TransfersView(
    onBackPress: () -> Unit,
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
    onNavigateToStorageSettings: () -> Unit,
    onSelectActiveTransfers: () -> Unit,
    onSelectCompletedTransfers: () -> Unit,
    onSelectTransfersClose: () -> Unit,
    onActiveTransferSelected: (InProgressTransfer) -> Unit,
    onCompletedTransferSelected: (CompletedTransfer) -> Unit,
    onSelectAllActiveTransfers: () -> Unit,
    onCancelSelectedActiveTransfers: () -> Unit,
    onClearSelectedCompletedTransfers: () -> Unit,
    onSelectAllCompletedTransfers: () -> Unit,
) = with(uiState) {
    var showActiveTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showCompletedTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showFailedTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showCancelAllTransfersDialog by rememberSaveable { mutableStateOf(false) }
    var showClearAllTransfersDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmCancelTransfersDialog by rememberSaveable { mutableStateOf(false) }

    @OptIn(ExperimentalMaterial3Api::class)
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_TRANSFERS_VIEW),
        topBar = {
            if (!isInSelectTransfersMode) {
                TransfersTopBar(onBackPress, getTransferActions(uiState)) { action ->
                    when (action) {
                        TransferMenuAction.Pause -> onPauseTransfers()
                        TransferMenuAction.Resume -> onResumeTransfers()
                        TransferMenuAction.More -> {
                            when (selectedTab) {
                                ACTIVE_TAB_INDEX -> showActiveTransfersModal = true
                                COMPLETED_TAB_INDEX -> showCompletedTransfersModal = true
                                FAILED_TAB_INDEX -> showFailedTransfersModal = true
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
                                ACTIVE_TAB_INDEX -> onSelectAllActiveTransfers()
                                COMPLETED_TAB_INDEX -> onSelectAllCompletedTransfers()
                                FAILED_TAB_INDEX -> {
                                    // TRAN-908
                                }
                            }
                        }

                        TransferMenuAction.CancelSelected, TransferMenuAction.ClearSelected -> {
                            when (selectedTab) {
                                ACTIVE_TAB_INDEX -> showConfirmCancelTransfersDialog = true
                                COMPLETED_TAB_INDEX -> onClearSelectedCompletedTransfers()
                                FAILED_TAB_INDEX -> {
                                    // TRAN-908
                                }
                            }
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
                pagerScrollEnabled = !isInSelectTransfersMode,
                cells = {
                    addTextTabWithLazyListState(
                        tabItem = TabItems(stringResource(id = sharedR.string.transfers_section_tab_title_active_transfers)),
                    ) { _, listState, modifier ->
                        ActiveTransfersView(
                            activeTransfers = activeTransfers,
                            isOverQuota = isOverQuota,
                            areTransfersPaused = areTransfersPaused,
                            onPlayPauseClicked = onPlayPauseTransfer,
                            onReorderPreview = onActiveTransfersReorderPreview,
                            onReorderConfirmed = onActiveTransfersReorderConfirmed,
                            onActiveTransferSelected = onActiveTransferSelected,
                            selectedActiveTransfersIds = selectedActiveTransfersIds,
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
                            modifier = modifier,
                        )
                    }
                },
                initialSelectedIndex = selectedTab,
                onTabSelected = {
                    onTabSelected(it)
                    true
                },
            )
        }

        if (showActiveTransfersModal) {
            ActiveTransfersActionsBottomSheet(
                onSelectTransfers = onSelectActiveTransfers,
                onCancelAllTransfers = { showCancelAllTransfersDialog = true },
                onDismissSheet = { showActiveTransfersModal = false },
            )
        }

        if (showCompletedTransfersModal) {
            CompletedTransfersActionsBottomSheet(
                onClearAllTransfers = { showClearAllTransfersDialog = true },
                onSelectTransfers = onSelectCompletedTransfers,
                onDismissSheet = { showCompletedTransfersModal = false },
            )
        }

        if (showFailedTransfersModal) {
            FailedTransfersActionsBottomSheet(
                onRetryAllTransfers = onRetryFailedTransfers,
                onClearAllTransfers = { showClearAllTransfersDialog = true },
                onDismissSheet = { showFailedTransfersModal = false },
            )
        }

        if (showCancelAllTransfersDialog) {
            CancelAllTransfersDialog(
                onCancelAllTransfers = onCancelAllFailedTransfers,
                onDismiss = { showCancelAllTransfersDialog = false },
            )
        }

        if (showClearAllTransfersDialog) {
            ClearAllTransfersDialog(
                onClearAllTransfers = {
                    when (selectedTab) {
                        COMPLETED_TAB_INDEX -> onClearAllCompletedTransfers()
                        FAILED_TAB_INDEX -> onClearAllFailedTransfers()
                    }
                },
                onDismiss = { showClearAllTransfersDialog = false },
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
            onNavigateToStorageSettings = {},
            onSelectActiveTransfers = {},
            onActiveTransferSelected = {},
            onSelectTransfersClose = {},
            onSelectAllActiveTransfers = {},
            onCancelSelectedActiveTransfers = {},
            onCompletedTransferSelected = {},
            onSelectAllCompletedTransfers = {},
            onClearSelectedCompletedTransfers = {},
            onSelectCompletedTransfers = {},
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
                add(TransferMenuAction.More)
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
