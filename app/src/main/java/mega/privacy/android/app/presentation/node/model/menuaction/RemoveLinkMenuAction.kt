package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
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
    override fun getDescription() = stringResource(id = R.string.context_remove_link_menu)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_remove_link)


    override val testTag: String = "menu_action:remove_link"
}