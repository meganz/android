package mega.privacy.android.app.presentation.transfers.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.view.inprogress.InProgressTransfersView
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.controls.tab.Tabs
import mega.privacy.android.shared.original.core.ui.controls.tab.TextCell
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialNavigationApi::class)
@Composable
internal fun TransfersView(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
    uiState: TransfersUiState,
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
                .semantics { testTagsAsResourceId = true },
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
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            ) {
                Tabs(
                    cells = persistentListOf(
                        TextCell(
                            text = stringResource(id = R.string.title_tab_in_progress_transfers),
                            tag = TEST_TAG_IN_PROGRESS_TAB,
                        ) {
                            InProgressTransfersView(
                                inProgressTransfers = inProgressTransfers,
                                isOverQuota = isOverQuota,
                                areTransfersPaused = areTransfersPaused,
                                onPlayPauseClicked = onPlayPauseTransfer,
                            )
                        },
                        TextCell(
                            text = stringResource(id = R.string.title_tab_completed_transfers),
                            tag = TEST_TAG_COMPLETED_TAB,
                        ) { CompletedTransfersView() }
                    ),
                    selectedIndex = selectedTab,
                )
            }
        }
    }
}

private fun getTransferActions(uiState: TransfersUiState) = with(uiState) {
    buildList {
        if (inProgressTransfers.isNotEmpty()) {
            if (selectedTab == IN_PROGRESS_TAB_INDEX) {
                if (areTransfersPaused) {
                    add(TransferMenuAction.Resume)
                } else {
                    add(TransferMenuAction.Pause)
                }
            }
            add(TransferMenuAction.More)
        }
    }
}

@Composable
internal fun CompletedTransfersView() {

}

@OptIn(ExperimentalMaterialNavigationApi::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkTransfersViewPreview")
@Composable
private fun TransfersViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransfersView(
            bottomSheetNavigator = rememberBottomSheetNavigator(),
            scaffoldState = rememberScaffoldState(),
            onBackPress = {},
            uiState = TransfersUiState(),
            onPlayPauseTransfer = {},
            onResumeTransfers = {},
            onPauseTransfers = {},
            onMoreInProgressActions = {},
        )
    }
}

/**
 * Tag for the in progress tab
 */
const val TEST_TAG_IN_PROGRESS_TAB = "transfers_view:tab_in_progress"

/**
 * Index for the in progress tab
 */
const val IN_PROGRESS_TAB_INDEX = 0

/**
 * Tag for the completed tab
 */
const val TEST_TAG_COMPLETED_TAB = "transfers_view:tab_completed"

/**
 * Index for the completed tab
 */
const val COMPLETED_TAB_INDEX = 1