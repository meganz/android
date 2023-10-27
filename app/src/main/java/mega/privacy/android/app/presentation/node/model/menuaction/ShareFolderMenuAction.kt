package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Share folder menu action
 */
class ShareFolderMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_share_folder)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_share)

    override val orderInCategory = 190

    override val testTag: String = "menu_action:share_folder"
}