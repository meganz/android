package mega.privacy.android.app.presentation.meeting.chat.view.actions


import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.toString
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.ContactAttachmentMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openSentRequests
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.ChatConversationInviteActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationInviteActionMenuItemEvent

/**
 *  Invite message action for bottom sheet and tool bar in select mode
 */
class InviteMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.contact_invite,
    icon = R.drawable.ic_icon_plus_circle_medium_regular_outline,
    testTag = "action_invite",
    group = MessageActionGroup.Contact,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>): Boolean = messages.isNotEmpty()
            && messages.all { it is ContactAttachmentMessage && !it.isMe && !it.isContact }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        val viewModel = hiltViewModel<ContactAttachmentMessageViewModel>()
        val snackBarHostState = LocalSnackBarHostState.current
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            launch {
                if (messages.size == 1) {
                    val result =
                        viewModel.inviteContact(messages.first() as ContactAttachmentMessage)

                    if (result is InviteUserAsContactResult.ContactInviteSent) {
                        snackBarHostState?.showAutoDurationSnackbar(
                            result.toString(context),
                            context.getString(R.string.action_see)
                        ).also { snackBarResult ->
                            if (snackBarResult == SnackbarResult.ActionPerformed) {
                                openSentRequests(context)
                            }
                        }
                    } else {
                        snackBarHostState?.showAutoDurationSnackbar(result.toString(context))
                    }
                } else {
                    viewModel.inviteMultipleContacts(messages as Set<ContactAttachmentMessage>)
                        .also { result -> snackBarHostState?.showAutoDurationSnackbar(result.toString(context)) }
                }
                Analytics.tracker.trackEvent(ChatConversationInviteActionMenuItemEvent)
                onHandled()
            }
        }
    }

    override fun trackTriggerEvent(source: TriggerSource) {
        when (source) {
            TriggerSource.BottomSheet -> {
                Analytics.tracker.trackEvent(ChatConversationInviteActionMenuItemEvent)
            }

            TriggerSource.Toolbar -> {
                Analytics.tracker.trackEvent(ChatConversationInviteActionMenuEvent)
            }
        }
    }
}
