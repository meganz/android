package mega.privacy.android.shared.nodes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedResR
import java.util.Locale

@Composable
fun NodeSelectionModeAppBar(
    count: Int,
    isAllSelected: Boolean,
    isSelecting: Boolean,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaTopAppBar(
        navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
        title = if (isSelecting) {
            stringResource(sharedResR.string.app_bar_selection_mode_description)
        } else {
            String.format(Locale.ROOT, "%s", count)
        },
        actions = buildList {
            if (isSelecting) {
                add(CommonMenuAction.Selecting)
            } else if (!isAllSelected) {
                add(CommonMenuAction.SelectAll)
            }
        },
        onActionPressed = {
            when (it) {
                is CommonMenuAction.SelectAll -> onSelectAllClicked()
                else -> Unit
            }
        }
    )
}