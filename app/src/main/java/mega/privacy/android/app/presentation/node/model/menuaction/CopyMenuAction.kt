package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Copy menu action
 */
class CopyMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_copy)

    @Composable
    override fun getIconPainter() = painterResource(id = iconPackR.drawable.ic_menu_copy)

    override val orderInCategory = 240

    override val testTag: String = "menu_action:copy"
}