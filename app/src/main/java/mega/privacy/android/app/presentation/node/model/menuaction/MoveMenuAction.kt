package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Move menu action
 */
class MoveMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.general_move)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_move_white)

    override val orderInCategory = 320

    override val testTag: String = "menu_action:move"
}