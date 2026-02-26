package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Available offline menu action
 */
class AvailableOfflineMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.CloudDownload)

    @Composable
    override fun getDescription() =
        stringResource(id = sharedR.string.node_menu_action_make_available_offline)

    override val orderInCategory: Int
        get() = 150
    override val testTag: String
        get() = "menu_action:available_offline"
}