package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Dispute take down menu action
 */
class DisputeTakeDownMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getDescription() = stringResource(id = R.string.dispute_takendown_file)

    @Composable
    override fun getIconPainter() = painterResource(id = R.drawable.ic_taken_down_menu_option)

    override val orderInCategory = 240

    override val testTag: String = "menu_action:Dispute_take_down"
}