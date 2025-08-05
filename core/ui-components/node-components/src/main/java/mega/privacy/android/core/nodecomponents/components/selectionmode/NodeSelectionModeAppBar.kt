package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.runtime.Composable
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import java.util.Locale

@Composable
fun NodeSelectionModeAppBar(
    count: Int,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
) {
    MegaTopAppBar(
        navigationType = AppBarNavigationType.Close(onCancelSelectionClicked),
        title = String.format(Locale.ROOT, "%s", count),
        actions = listOf(
            NodeSelectionAction.SelectAll
        ),
        onActionPressed = {
            when (it) {
                is NodeSelectionAction.SelectAll -> onSelectAllClicked()
                else -> Unit
            }
        }
    )
}

