package mega.privacy.android.app.presentation.node.model.menuaction

import mega.privacy.android.core.R as CoreUiR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Send to chat menu action
 */
class SendMenuAction @Inject constructor(override val enabled: Boolean) : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_send)

    @Composable
    override fun getIconPainter() =
        painterResource(id = CoreUiR.drawable.ic_send_horizontal)

    override val orderInCategory = 180

    override val highlightIcon = true

    override val testTag: String = "menu_action:send"
}