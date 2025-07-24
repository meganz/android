package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Delete bottom sheet menu action
 */
class DeletePermanentlyMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.X)

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.rubbish_bin_bottom_menu_option_delete)

    override val orderInCategory: Int
        get() = 280

    override val testTag: String
        get() = "menu_action:delete"
}