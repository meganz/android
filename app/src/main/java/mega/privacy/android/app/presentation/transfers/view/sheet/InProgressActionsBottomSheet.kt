package mega.privacy.android.app.presentation.transfers.view.sheet

import mega.privacy.android.icon.pack.R.drawable as iconPack
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Bottom sheet for in progress transfers actions.
 */
@Composable
fun InProgressActionsBottomSheet(
    onCancelAllTransfers: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .testTag(TEST_TAG_IN_PROGRESS_ACTIONS_PANEL)
) {
    MenuActionListTile(
        modifier = Modifier.testTag(TEST_TAG_CANCEL_ALL_ACTION),
        text = stringResource(id = R.string.menu_cancel_all_transfers),
        icon = painterResource(id = iconPack.ic_x_circle_medium_regular_outline),
        dividerType = null,
        onActionClicked = onCancelAllTransfers,
    )
}

@CombinedThemePreviews
@Composable
private fun InProgressActionsBottomSheetPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        InProgressActionsBottomSheet(
            onCancelAllTransfers = {},
        )
    }
}

internal const val TEST_TAG_IN_PROGRESS_ACTIONS_PANEL = "transfers_view:in_progress_actions_panel"
internal const val TEST_TAG_CANCEL_ALL_ACTION =
    "transfers_view:in_progress_actions_panel:cancel_all_action"