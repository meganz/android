package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Get link menu action
 */
class GetLinkMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getDescription() =
        pluralStringResource(id = sharedR.plurals.label_share_links, count = 1)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01)


    override val orderInCategory = 160

    override val testTag: String = "menu_action:get_link"
}