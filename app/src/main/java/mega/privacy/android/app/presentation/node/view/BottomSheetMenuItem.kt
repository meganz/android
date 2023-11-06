package mega.privacy.android.app.presentation.node.view

import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.BottomSheetClickHandler

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
    val control: BottomSheetClickHandler,
)
