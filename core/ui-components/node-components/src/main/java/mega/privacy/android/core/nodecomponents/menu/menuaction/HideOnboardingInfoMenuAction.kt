package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.shared.resources.R as SharedResR
import javax.inject.Inject

/**
 * Menu action dispatched by the help icon on the hide menu item to open the
 * Hidden Nodes onboarding screen without hiding the node.
 *
 * It is triggered imperatively and never rendered in a menu list.
 *
 * @property orderInCategory
 */
class HideOnboardingInfoMenuAction @Inject constructor() : MenuAction {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.general_hide_node)

    override val testTag: String = "menu_action:hide_onboarding_info"

    override val orderInCategory: Int
        get() = 220
}
