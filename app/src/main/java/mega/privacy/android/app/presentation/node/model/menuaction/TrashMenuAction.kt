package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Rubbish bin menu action
 */
class TrashMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_move_to_rubbish_bin)

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_move_to_trash)

    override val orderInCategory = 350

    override val testTag: String = "menu_action:rubbish_bin"
}