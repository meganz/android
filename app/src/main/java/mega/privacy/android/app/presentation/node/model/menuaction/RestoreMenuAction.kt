package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Restore menu action
 */
class RestoreMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_restore)

    @Composable
    override fun getIconPainter() =
        painterResource(id = IconPackR.drawable.ic_rotate_ccw_medium_regular_outline)

    override val orderInCategory = 250

    override val testTag: String = "menu_action:restore"
}