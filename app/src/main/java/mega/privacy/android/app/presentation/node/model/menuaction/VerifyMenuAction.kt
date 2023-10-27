package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Verify contact menu action
 */
class VerifyMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_change_permissions)

    @Composable
    override fun getDescription() =
        stringResource(id = R.string.shared_items_bottom_sheet_menu_verify_user)

    override val orderInCategory = 40

    override val testTag: String
        get() = "menu_action:verify"
}