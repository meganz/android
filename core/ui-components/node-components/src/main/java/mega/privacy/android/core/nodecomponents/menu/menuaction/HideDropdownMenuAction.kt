package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.android.core.ui.model.menu.MenuAction
import javax.inject.Inject

/**
 * Hide menu action
 *
 * @property orderInCategory
 */
class HideDropdownMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.general_hide_node)

    override val testTag: String = "menu_action:hide"

    override val orderInCategory: Int
        get() = 25
}