package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimelineTabActionBottomSheet(
    actions: ImmutableList<TimelineSelectionMenuAction>,
    onDismissRequest: () -> Unit,
    onActionPressed: (TimelineSelectionMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        modifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        actions.forEach { action ->
            NodeActionListTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                menuAction = action,
                onActionClicked = {
                    onActionPressed(action)
                    onDismissRequest()
                },
                isDestructive = action.highlightIcon
            )
        }
    }
}
