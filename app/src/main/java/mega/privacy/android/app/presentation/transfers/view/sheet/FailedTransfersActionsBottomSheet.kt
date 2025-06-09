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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.failed.TEST_TAG_FAILED_TRANSFERS_VIEW
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Bottom sheet for in progress transfers actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailedTransfersActionsBottomSheet(
    onRetryAllTransfers: () -> Unit,
    onClearAllTransfers: () -> Unit,
    onSelectTransfers: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) = MegaModalBottomSheet(
    bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
    onDismissRequest = onDismissSheet,
    modifier = modifier
        .fillMaxWidth()
        .semantics { testTagsAsResourceId = true }
        .testTag(TEST_TAG_FAILED_ACTIONS_PANEL),
    sheetState = sheetState,
) {
    OneLineListItem(
        modifier = Modifier.testTag(TEST_TAG_SELECT_ACTION),
        text = stringResource(id = mega.privacy.android.shared.resources.R.string.general_select),
        onClickListener = {
            onSelectTransfers()
            onDismissSheet()
        },
    )
    OneLineListItem(
        modifier = Modifier.testTag(TEST_TAG_RETRY_ALL_ACTION),
        text = stringResource(id = R.string.option_to_retry_transfers),
        onClickListener = {
            onRetryAllTransfers()
            onDismissSheet()
        },
    )
    OneLineListItem(
        modifier = Modifier.testTag(TEST_TAG_CLEAR_ALL_FAILED_ACTION),
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
private fun FailedTransfersActionsBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FailedTransfersActionsBottomSheet(
            onRetryAllTransfers = {},
            onClearAllTransfers = {},
            onSelectTransfers = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_FAILED_ACTIONS_PANEL =
    "$TEST_TAG_FAILED_TRANSFERS_VIEW:actions_panel"
internal const val TEST_TAG_CLEAR_ALL_FAILED_ACTION =
    "$TEST_TAG_FAILED_ACTIONS_PANEL:clear_all_action"
internal const val TEST_TAG_RETRY_ALL_ACTION = "$TEST_TAG_FAILED_ACTIONS_PANEL:retry_all_action"