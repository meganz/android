package mega.privacy.android.app.presentation.node.view

import androidx.compose.runtime.Composable
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Bottom sheet menu item
 *
 * @property group
 * @property orderInGroup
 * @property control
 */
data class BottomSheetMenuItem(
    val group: Int,
    val orderInGroup: Int,
    val control: @Composable (onDismiss: () -> Unit, actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit) -> Unit,
)