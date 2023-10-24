package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import javax.inject.Inject

/**
 * Clear selection menu action
 */
class ClearSelectionMenuAction @Inject constructor() : MenuAction {
    @Composable
    override fun getDescription() = stringResource(id = R.string.action_unselect_all)

    override val orderInCategory = 20

    override val testTag: String = "menu_action:clear_selection"
}
