package mega.privacy.android.core.nodecomponents.menu.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as SharedResR
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.R
import javax.inject.Inject

/**
 * Send to chat menu action
 */
class SendMenuAction @Inject constructor(override val enabled: Boolean) : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = SharedResR.string.context_send)

    @Composable
    override fun getIconPainter() =
        painterResource(id = R.drawable.ic_send_horizontal)

    override val orderInCategory = 180

    override val highlightIcon = true

    override val testTag: String = "menu_action:send"
}