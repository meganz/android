package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import javax.inject.Inject

/**
 * Remove share menu action
 */
class RemoveShareDropdownMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_clean_shares_menu)


    override val testTag: String = "menu_action:remove_share"

    override val orderInCategory = 210
}