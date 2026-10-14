package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.ChatRichLinkMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.openMessageLink
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostStateOriginal
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.core.R as CoreR

/**
 * Chat rich link ui message
 *
 * @property message
 * @property showAvatar
 */
data class ChatRichLinkUiMessage(
    override val message: RichPreviewMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
        navHostController: NavHostController,
    ) {
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = LocalSnackBarHostStateOriginal.current
        val failureMessage =
            stringResource(id = CoreR.string.chat_click_link_in_message_intent_not_available)
        val coroutineScope = rememberCoroutineScope()

        ChatRichLinkMessageView(
            message = message,
            interactionEnabled = interactionEnabled,
            onLongClick = onLongClick,
            modifier = initialiseModifier {
                message.chatRichPreviewInfo?.url?.let { link ->
                    coroutineScope.launch {
                        openMessageLink(
                            link = link,
                            uriHandler = uriHandler,
                            snackbarHostState = snackbarHostState,
                            failureMessage = failureMessage,
                        )
                    }
                }
            },
        )
    }

    override val modifier: Modifier
        get() = if (message.isMine) {
            Modifier
                .padding(start = 8.dp)
                .fillMaxWidth()
        } else {
            Modifier
                .padding(end = 8.dp)
                .fillMaxWidth()
        }

    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}