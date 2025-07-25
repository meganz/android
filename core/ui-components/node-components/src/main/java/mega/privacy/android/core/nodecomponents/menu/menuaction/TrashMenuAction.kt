package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Rubbish bin menu action
 */
class TrashMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

    @Composable
    override fun getDescription() =
        stringResource(id = SharedResR.string.node_option_move_to_rubbish_bin)

    override val orderInCategory = 270

    override val testTag: String = "menu_action:rubbish_bin"
}