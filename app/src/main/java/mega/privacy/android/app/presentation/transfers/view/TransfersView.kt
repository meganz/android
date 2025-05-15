package mega.privacy.android.app.presentation.transfers.view

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.view.active.ActiveTransfersView
import mega.privacy.android.app.presentation.transfers.view.completed.CompletedTransfersView
import mega.privacy.android.app.presentation.transfers.view.failed.FailedTransfersView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun TransfersView(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
    uiState: TransfersUiState,
    onTabSelected: (Int) -> Unit,
    onPlayPauseTransfer: (Int) -> Unit,
    onResumeTransfers: () -> Unit,
    onPauseTransfers: () -> Unit,
    onMoreInProgressActions: () -> Unit,
) = with(uiState) {
    MegaBottomSheetLayout(
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
        },
        bottomSheetNavigator = bottomSheetNavigator
    ) {
        MegaScaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .semantics { testTagsAsResourceId = true }
                .testTag(TEST_TAG_TRANSFERS_VIEW),
            scaffoldState = scaffoldState,
            topBar = {
                MegaAppBar(
                    appBarType = AppBarType.BACK_NAVIGATION,
                    title = stringResource(id = R.string.section_transfers),
                    onNavigationPressed = onBackPress,
                    actions = getTransferActions(uiState),
                    onActionPressed = { action ->
                        when (action) {
                            TransferMenuAction.Pause -> onPauseTransfers()
                            TransferMenuAction.Resume -> onResumeTransfers()
                            TransferMenuAction.More -> onMoreInProgressActions()
                        }
                    },
                    elevation = 0.dp,
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
        if (selectedTab == ACTIVE_TAB_INDEX && activeTransfers.isNotEmpty()) {
            if (areTransfersPaused) {
                add(TransferMenuAction.Resume)
            } else {
                add(TransferMenuAction.Pause)
            }
            add(TransferMenuAction.More)
        }
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class)
@CombinedThemePreviews
@Composable
private fun TransfersViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        TransfersView(
            bottomSheetNavigator = rememberBottomSheetNavigator(),
            scaffoldState = rememberScaffoldState(),
            onBackPress = {},
            uiState = TransfersUiState(),
            onTabSelected = {},
            onPlayPauseTransfer = {},
            onResumeTransfers = {},
            onPauseTransfers = {},
            onMoreInProgressActions = {},
        )
    }
}

const val TEST_TAG_TRANSFERS_VIEW = "transfers_view"

const val TEST_TAG_EMPTY_TRANSFERS_VIEW = "$TEST_TAG_TRANSFERS_VIEW:empty"

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
