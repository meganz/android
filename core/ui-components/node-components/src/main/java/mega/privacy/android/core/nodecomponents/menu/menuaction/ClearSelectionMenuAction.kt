package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.shared.resources.R as SharedResR
import javax.inject.Inject

/**
 * Clear selection menu action
 */
class ClearSelectionMenuAction @Inject constructor() : MenuAction {
    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.action_unselect_all)

    override val orderInCategory = 20

    override val testTag: String = "menu_action:clear_selection"
}
