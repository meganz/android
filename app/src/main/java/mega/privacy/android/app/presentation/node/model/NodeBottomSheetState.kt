package mega.privacy.android.app.presentation.node.model

import mega.privacy.android.app.presentation.node.view.BottomSheetMenuItem

/**
 * Node bottom sheet state
 *
 * @property name
 * @property actions
 */
data class NodeBottomSheetState(
    val name: String = "",
    val actions: List<BottomSheetMenuItem> = emptyList(),
)