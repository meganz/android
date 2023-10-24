package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Manage link menu action
 *
 * @property orderInCategory
 */
class ManageLinkMenuAction @Inject constructor(
    override val orderInCategory: Int,
) : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        painterResource(id = R.drawable.link_ic_white)

    @Composable
    override fun getDescription() =
        stringResource(id = mega.privacy.android.app.R.string.edit_link_option)


    override val testTag: String = "menu_action:manage_link"
}