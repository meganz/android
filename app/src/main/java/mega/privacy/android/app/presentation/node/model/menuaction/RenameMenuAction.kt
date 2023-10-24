package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Rename menu action
 *
 * @property orderInCategory
 */
class RenameMenuAction @Inject constructor(
    override val orderInCategory: Int,
) : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_rename)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_rename)

    override val testTag: String = "menu_action:rename"
}