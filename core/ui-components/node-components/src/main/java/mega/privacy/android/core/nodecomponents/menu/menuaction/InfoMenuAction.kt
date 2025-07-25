package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Info menu action
 */
class InfoMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info)

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.general_info)

    override val orderInCategory = 50
    override val testTag: String
        get() = "menu_action:info"
}