package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Manage link menu action
 *
 * @property orderInCategory
 */
class ManageLinkMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getIconPainter() =
        painterResource(id = iconPackR.drawable.ic_link01_medium_regular_outline)

    @Composable
    override fun getDescription() =
        stringResource(id = mega.privacy.android.app.R.string.edit_link_option)


    override val testTag: String = "menu_action:manage_link"

    override val orderInCategory: Int
        get() = 160
}