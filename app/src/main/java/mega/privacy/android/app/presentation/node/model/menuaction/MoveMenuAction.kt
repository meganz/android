package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Move menu action
 */
class MoveMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.general_move)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Move)

    override val orderInCategory = 230

    override val testTag: String = "menu_action:move"
}