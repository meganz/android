package mega.privacy.android.core.nodecomponents.model

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
    val control: BottomSheetClickHandler,
)