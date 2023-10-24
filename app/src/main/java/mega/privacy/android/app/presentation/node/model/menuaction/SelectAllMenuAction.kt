package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import javax.inject.Inject

/**
 * Select all menu action
 */
class SelectAllMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = R.string.action_select_all)

    override val orderInCategory = 10

    override val testTag: String = "menu_type:select_all"
}