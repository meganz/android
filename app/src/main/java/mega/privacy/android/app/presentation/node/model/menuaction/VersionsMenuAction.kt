package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Versions menu action
 */
class VersionsMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = iconPackR.drawable.ic_menu_versions)

    @Composable
    override fun getDescription() = stringResource(id = R.string.title_section_versions)

    override val orderInCategory: Int
        get() = 70
    override val testTag: String
        get() = "menu_action:versions"
}