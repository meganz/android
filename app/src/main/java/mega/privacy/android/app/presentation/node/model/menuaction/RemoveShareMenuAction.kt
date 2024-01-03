package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Remove share menu action
 *
 * @property orderInCategory
 */
class RemoveShareMenuAction @Inject constructor(
    override val orderInCategory: Int,
) : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() = painterResource(id = iconPackR.drawable.ic_menu_remove_share)

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_clean_shares_menu)


    override val testTag: String = "menu_action:remove_share"
}