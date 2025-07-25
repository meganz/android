package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as SharedResR
import javax.inject.Inject

/**
 * Available offline menu action
 */
class AvailableOfflineMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.ArrowDownCircle)

    @Composable
    override fun getDescription() =
        stringResource(id = SharedResR.string.file_properties_available_offline)

    override val orderInCategory: Int
        get() = 150
    override val testTag: String
        get() = "menu_action:available_offline"
}