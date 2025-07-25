package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.android.core.ui.model.menu.MenuAction
import javax.inject.Inject

/**
 * Select all menu action
 */
class SelectAllMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.action_select_all)

    override val orderInCategory = 10

    override val testTag: String = "menu_type:select_all"
}