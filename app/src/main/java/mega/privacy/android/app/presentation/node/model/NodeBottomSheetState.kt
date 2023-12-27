package mega.privacy.android.app.presentation.node.model

import mega.privacy.android.app.presentation.node.view.BottomSheetMenuItem

/**
 * Node bottom sheet state
 *
 * @property name
 * @property isOnline
 * @property actions
 */
data class NodeBottomSheetState(
    val name: String = "",
    val isOnline: Boolean = false,
    val actions: List<BottomSheetMenuItem> = emptyList(),
)