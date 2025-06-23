package mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.TEST_TAG_ACTIVE_TAB
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ActiveTransfersCancelAllMenuItemEvent
import mega.privacy.mobile.analytics.event.ActiveTransfersSelectMenuItemEvent

/**
 * Bottom sheet for active transfers actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTransfersActionsBottomSheet(
    onSelectTransfers: () -> Unit,
    onCancelAllTransfers: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) = MegaModalBottomSheet(
    bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
    onDismissRequest = onDismissSheet,
    modifier = modifier
        .fillMaxWidth()
        .testTag(TEST_TAG_ACTIVE_ACTIONS_PANEL),
    sheetState = sheetState,
) {
    OneLineListItem(
        modifier = Modifier.testTag(TEST_TAG_SELECT_ACTION),
        text = stringResource(id = sharedR.string.general_select),
        onClickListener = {
            Analytics.tracker.trackEvent(ActiveTransfersSelectMenuItemEvent)
            onSelectTransfers()
            onDismissSheet()
        },
    )
    OneLineListItem(
        modifier = Modifier.testTag(TEST_TAG_CANCEL_ALL_ACTION),
        text = stringResource(id = R.string.menu_cancel_all_transfers),
        onClickListener = {
            Analytics.tracker.trackEvent(ActiveTransfersCancelAllMenuItemEvent)
            onCancelAllTransfers()
            onDismissSheet()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun InProgressActionsBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ActiveTransfersActionsBottomSheet(
            onSelectTransfers = {},
            onCancelAllTransfers = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_ACTIVE_ACTIONS_PANEL = "$TEST_TAG_ACTIVE_TAB:actions_panel"
internal const val TEST_TAG_CANCEL_ALL_ACTION = "$TEST_TAG_ACTIVE_ACTIONS_PANEL:cancel_all_action"
internal const val TEST_TAG_SELECT_ACTION = "$TEST_TAG_ACTIVE_ACTIONS_PANEL:select_all_action"