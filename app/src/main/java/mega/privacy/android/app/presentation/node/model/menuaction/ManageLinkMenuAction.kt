package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Manage link menu action
 *
 * @property orderInCategory
 */
class ManageLinkMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Solid.Link01)

    @Composable
    override fun getDescription() =
        stringResource(id = mega.privacy.android.app.R.string.edit_link_option)


    override val testTag: String = "menu_action:manage_link"

    override val orderInCategory: Int
        get() = 160
}