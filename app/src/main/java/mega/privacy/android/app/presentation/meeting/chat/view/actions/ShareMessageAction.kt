package mega.privacy.android.app.presentation.meeting.chat.view.actions

import mega.privacy.android.icon.pack.R as iconPackR
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationShareActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationShareActionMenuItemEvent
import timber.log.Timber

internal class ShareMessageAction : MessageAction(
    text = R.string.general_share,
    icon = iconPackR.drawable.ic_share_network_medium_regular_outline,
    testTag = "action_share",
    group = MessageActionGroup.Share,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty() &&
            messages.all { it is NodeAttachmentMessage && it.exists }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        val viewModel = hiltViewModel<NodeAttachmentMessageViewModel>()
        val context = LocalContext.current
        LaunchedEffect(messages) {
            runCatching {
                val fileNodes = messages.map { (it as NodeAttachmentMessage).fileNode }
                val uris = viewModel.getShareChatNodes(fileNodes)
                val intent = viewModel.getShareIntent(fileNodes, uris)
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.getString(R.string.context_share)
                    )
                )
            }.onFailure {
                Timber.e(it, "Failed to share message")
            }
            onHandled()
        }
    }

    override fun trackTriggerEvent(source: TriggerSource) {
        when (source) {
            TriggerSource.BottomSheet -> {
                Analytics.tracker.trackEvent(ChatConversationShareActionMenuItemEvent)
            }

            TriggerSource.Toolbar -> {
                Analytics.tracker.trackEvent(ChatConversationShareActionMenuEvent)
            }
        }
    }
}