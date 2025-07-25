package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.android.core.ui.model.menu.MenuAction
import javax.inject.Inject

/**
 * Remove link menu action
 *
 * @property orderInCategory
 */
class RemoveLinkDropdownMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.context_remove_link_menu)

    override val testTag: String = "menu_action:remove_link"

    override val orderInCategory: Int
        get() = 170
}