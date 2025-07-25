package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Remove link menu action
 *
 * @property orderInCategory
 */
class RemoveLinkMenuAction @Inject constructor(
    override val orderInCategory: Int,
) : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.context_remove_link_menu)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.LinkOff01)


    override val testTag: String = "menu_action:remove_link"
}