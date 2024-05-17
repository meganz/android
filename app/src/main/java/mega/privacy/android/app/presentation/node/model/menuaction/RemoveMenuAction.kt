package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Remove node menu action
 */
class RemoveMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.general_remove)

    @Composable
    override fun getIconPainter() = painterResource(id = iconPackR.drawable.ic_x_medium_regular_outline)

    override val orderInCategory = 230

    override val testTag: String = "menu_action:remove_node"
}