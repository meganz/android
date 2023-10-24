package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Remove node menu action
 */
class RemoveMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.general_remove)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_remove)

    override val orderInCategory = 230

    override val testTag: String = "menu_action:remove_node"
}