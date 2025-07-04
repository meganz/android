package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Send to chat menu action
 */
class SendToChatMenuAction @Inject constructor() : MenuActionWithIcon {

    @Composable
    override fun getDescription() = stringResource(id = R.string.context_send_file_to_chat)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Regular.Outline.MessageArrowUp)

    override val orderInCategory = 180

    override val testTag: String = "menu_action:send_to_chat"
}