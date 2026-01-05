package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoPlaylistBottomSheet(
    actions: List<MenuActionWithIcon>,
    sheetState: SheetState,
    onActionClicked: (MenuActionWithIcon) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaModalBottomSheet(
        modifier = modifier,
        bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        content = {
            actions.forEach { action ->
                NodeActionListTile(
                    modifier = Modifier.testTag(action.testTag),
                    menuAction = action,
                    onActionClicked = { onActionClicked(action) }
                )
            }
        }
    )
}