package mega.privacy.android.app.presentation.node.view.toolbar

import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem

/**
 * State for toolbar
 * @property toolbarMenuItems List of [ToolbarMenuItem]
 */
data class ToolbarState(
    val toolbarMenuItems: List<ToolbarMenuItem> = emptyList(),
)