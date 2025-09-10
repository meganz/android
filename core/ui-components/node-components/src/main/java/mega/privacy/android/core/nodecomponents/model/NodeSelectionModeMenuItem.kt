package mega.privacy.android.core.nodecomponents.model

import mega.android.core.ui.model.menu.MenuActionWithIcon

data class NodeSelectionModeMenuItem(
    val action: MenuActionWithIcon,
    val handler: NodeSelectionHandler,
)