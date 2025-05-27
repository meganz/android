package mega.privacy.android.app.presentation.transfers.view

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.view.active.ActiveTransfersView
import mega.privacy.android.app.presentation.transfers.view.completed.CompletedTransfersView
import mega.privacy.android.app.presentation.transfers.view.dialog.CancelAllTransfersDialog
import mega.privacy.android.app.presentation.transfers.view.dialog.ClearAllTransfersDialog
import mega.privacy.android.app.presentation.transfers.view.failed.FailedTransfersView
import mega.privacy.android.app.presentation.transfers.view.sheet.ActiveTransfersActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.CompletedTransfersActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.FailedTransfersActionsBottomSheet
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
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
) = with(uiState) {
    var showActiveTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showCompletedTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showFailedTransfersModal by rememberSaveable { mutableStateOf(false) }
    var showCancelAllTransfersDialog by rememberSaveable { mutableStateOf(false) }
    var showClearAllTransfersDialog by rememberSaveable { mutableStateOf(false) }

    MegaScaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_TRANSFERS_VIEW),
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back(onBackPress),
                title = stringResource(id = R.string.section_transfers),
                actions = getTransferActions(uiState),
                onActionPressed = { action ->
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
                },
            )
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
                cells = {
                    addTextTab(
                        tabItem = TabItems(stringResource(id = sharedR.string.transfers_section_tab_title_active_transfers)),
                    ) {
                        ActiveTransfersView(
                            activeTransfers = activeTransfers,
                            isOverQuota = isOverQuota,
                            areTransfersPaused = areTransfersPaused,
                            onPlayPauseClicked = onPlayPauseTransfer,
                            onReorderPreview = onActiveTransfersReorderPreview,
                            onReorderConfirmed = onActiveTransfersReorderConfirmed,
                        )
                    }
                    addTextTab(
                        tabItem = TabItems(stringResource(id = R.string.title_tab_completed_transfers)),
                    ) {
                        CompletedTransfersView(
                            completedTransfers = completedTransfers,
                        )
                    }
                    addTextTab(
                        tabItem = TabItems(stringResource(id = sharedR.string.transfers_section_tab_title_failed_transfers)),
                    ) {
                        FailedTransfersView(
                            failedTransfers = failedTransfers,
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
                onSelectTransfers = {},
                onCancelAllTransfers = { showCancelAllTransfersDialog = true },
                onDismissSheet = { showActiveTransfersModal = false },
            )
        }

        if (showCompletedTransfersModal) {
            CompletedTransfersActionsBottomSheet(
                onClearAllTransfers = { showClearAllTransfersDialog = true },
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

private fun getTransferActions(uiState: TransfersUiState) = with(uiState) {
    buildList {
        when {
            selectedTab == ACTIVE_TAB_INDEX && activeTransfers.isNotEmpty() -> {
                if (areTransfersPaused) {
                    add(TransferMenuAction.Resume)
                } else {
                    add(TransferMenuAction.Pause)
                }
                add(TransferMenuAction.More)
            }

            selectedTab == COMPLETED_TAB_INDEX && completedTransfers.isNotEmpty() -> {
                add(TransferMenuAction.More)
            }

            selectedTab == FAILED_TAB_INDEX && failedTransfers.isNotEmpty() -> {
                add(TransferMenuAction.More)
            }
        }
    }
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
        )
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
