package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.android.core.ui.model.menu.MenuAction
import javax.inject.Inject

/**
 * Hide menu action
 *
 * @property orderInCategory
 */
class HideDropdownMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = R.string.general_hide_node)

    override val testTag: String = "menu_action:hide"

    override val orderInCategory: Int
        get() = 25
}