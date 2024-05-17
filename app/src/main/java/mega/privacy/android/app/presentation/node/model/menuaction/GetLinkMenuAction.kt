package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Get link menu action
 */
class GetLinkMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getDescription() =
        pluralStringResource(id = sharedR.plurals.label_share_links, count = 1)

    @Composable
    override fun getIconPainter() =
        painterResource(id = iconPackR.drawable.ic_link_01_medium_regular_outline)

    override val orderInCategory = 160

    override val testTag: String = "menu_action:get_link"
}