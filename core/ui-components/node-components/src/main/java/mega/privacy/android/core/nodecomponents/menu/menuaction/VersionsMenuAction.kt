package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuActionWithIcon
import javax.inject.Inject

/**
 * Versions menu action
 */
class VersionsMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() =
        rememberVectorPainter(IconPack.Medium.Thin.Outline.ClockRotate)

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.title_section_versions)

    override val orderInCategory: Int
        get() = 70
    override val testTag: String
        get() = "menu_action:versions"
}