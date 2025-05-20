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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.completed.TEST_TAG_COMPLETED_TRANSFERS_VIEW
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Bottom sheet for completed transfers actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTransfersActionsBottomSheet(
    onClearAllTransfers: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) = MegaModalBottomSheet(
    bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
    onDismissRequest = onDismissSheet,
    modifier = modifier
        .fillMaxWidth()
        .testTag(TEST_TAG_COMPLETED_ACTIONS_PANEL),
    sheetState = sheetState,
) {
    OneLineListItem(
        modifier = Modifier.testTag(TEST_TAG_CLEAR_ALL_COMPLETED_ACTION),
        text = stringResource(id = R.string.option_to_clear_transfers),
        onClickListener = {
            onClearAllTransfers()
            onDismissSheet()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun CompletedTransfersActionsBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CompletedTransfersActionsBottomSheet(
            onClearAllTransfers = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_COMPLETED_ACTIONS_PANEL =
    "$TEST_TAG_COMPLETED_TRANSFERS_VIEW:actions_panel"
internal const val TEST_TAG_CLEAR_ALL_COMPLETED_ACTION =
    "$TEST_TAG_COMPLETED_ACTIONS_PANEL:clear_all_action"