package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Leave share menu action
 */
class LeaveShareMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.alert_leave_share)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(
        IconPack.Medium.Regular.Outline.LogOut02)

    override val orderInCategory = 260

    override val testTag: String = "menu_action:leave_share"
}