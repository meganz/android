package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Leave share menu action
 */
class LeaveShareMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.alert_leave_share)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_leave_share_w)

    override val orderInCategory = 340

    override val testTag: String = "menu_action:leave_share"
}