package mega.privacy.android.core.nodecomponents.model

import androidx.compose.runtime.Composable

/**
 * Bottom sheet menu item
 *
 * @property group
 * @property orderInGroup
 * @property control
 */
data class NodeActionModeMenuItem(
    val group: Int,
    val orderInGroup: Int,
    val control: @Composable (BottomSheetClickHandler) -> Unit,
)