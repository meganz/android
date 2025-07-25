package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.android.core.ui.model.menu.MenuAction
import javax.inject.Inject

/**
 * Remove share menu action
 */
class RemoveShareDropdownMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.context_clean_shares_menu)


    override val testTag: String = "menu_action:remove_share"

    override val orderInCategory = 210
}