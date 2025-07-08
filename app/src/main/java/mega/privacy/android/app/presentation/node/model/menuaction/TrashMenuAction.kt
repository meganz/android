package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Rubbish bin menu action
 */
class TrashMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.Trash)

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_move_to_trash)

    override val orderInCategory = 270

    override val testTag: String = "menu_action:rubbish_bin"
}