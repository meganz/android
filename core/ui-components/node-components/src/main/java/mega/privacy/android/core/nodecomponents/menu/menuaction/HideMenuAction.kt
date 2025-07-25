package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Hide menu action
 *
 * @property orderInCategory
 */
class HideMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.general_hide_node)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.EyeOff)

    override val testTag: String = "menu_action:hide"

    override val orderInCategory: Int
        get() = 220
}