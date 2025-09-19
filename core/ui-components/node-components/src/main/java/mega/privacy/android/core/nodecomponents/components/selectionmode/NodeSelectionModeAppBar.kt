package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.shared.resources.R as sharedResR
import java.util.Locale

@Composable
fun NodeSelectionModeAppBar(
    count: Int,
    isSelecting: Boolean,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
) {
    MegaTopAppBar(
        navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
        title = if (isSelecting) {
            stringResource(sharedResR.string.app_bar_selection_mode_description)
        } else {
            String.format(Locale.ROOT, "%s", count)
        },
        actions = if (isSelecting) {
            listOf(NodeSelectionAction.Selecting)
        } else {
            listOf(NodeSelectionAction.SelectAll)
        },
        onActionPressed = {
            when (it) {
                is NodeSelectionAction.SelectAll -> onSelectAllClicked()
                else -> Unit
            }
        }
    )
}

