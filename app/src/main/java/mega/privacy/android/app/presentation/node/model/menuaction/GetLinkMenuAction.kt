package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Get link menu action
 */
class GetLinkMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getDescription() = pluralStringResource(id = R.plurals.get_links, count = 1)

    @Composable
    override fun getIconPainter() =
        painterResource(id = mega.privacy.android.core.R.drawable.link_ic_white)

    override val orderInCategory = 160

    override val testTag: String = "menu_action:get_link"
}