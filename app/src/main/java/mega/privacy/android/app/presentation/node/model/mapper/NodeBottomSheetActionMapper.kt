package mega.privacy.android.app.presentation.node.model.mapper

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.node.model.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Node bottom sheet action mapper
 *
 * Helps to get bottom sheet actions based on the nodes selected
 */
class NodeBottomSheetActionMapper @Inject constructor() {

    /**
     * Invocation
     *
     * @param toolbarOptions all the toolbar options available for selected screen
     * @param selectedNode selected node
     */
    operator fun invoke(
        toolbarOptions: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<*>>,
        selectedNode: TypedNode,
    ): List<@Composable ((MenuAction) -> Unit) -> Unit> = toolbarOptions
        .mapNotNull {
            if (it.shouldDisplay()) {
                it.menuAction(selectedNode)
            } else null
        }
}