package mega.privacy.android.app.presentation.meeting.chat.model.messages

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.toUiChatStatus
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactAttachmentMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openSentRequests
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.original.core.ui.controls.chat.UiChatStatus
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Contact attachment ui message
 *
 * @property message
 * @property showAvatar
 */
data class ContactAttachmentUiMessage(
    override val message: ContactAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
        navHostController: NavHostController,
    ) {
        val viewModel: ContactMessageViewModel = hiltViewModel()
        var status by remember { mutableStateOf<UiChatStatus?>(null) }
        var userName by remember { mutableStateOf(message.contactUserName) }
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackBarHostState.current
        val onClick = {
            onUserClick(
                handle = message.contactHandle,
                email = message.contactEmail,
                name = message.contactUserName,
                isContact = message.isContact,
                context = context,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                checkContact = viewModel::checkUser,
                inviteUser = viewModel::inviteUser,
            )
        }

        LaunchedEffect(message.contactEmail) {
            val item = viewModel.loadContactInfo(
                contactEmail = message.contactEmail
            )
            status = item?.status?.takeIf {
                item.visibility == UserVisibility.Visible
            }?.toUiChatStatus()
            userName =
                item?.contactData?.alias ?: item?.contactData?.fullName ?: message.contactUserName
        }

        ContactAttachmentMessageView(
            message = message,
            userName = userName,
            status = status,
            modifier = initialiseModifier(onClick),
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

internal fun onUserClick(
    handle: Long,
    email: String,
    name: String,
    isContact: Boolean,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState?,
    checkContact: (Long, String, Boolean, () -> Unit, () -> Unit, () -> Unit) -> Unit,
    inviteUser: (String, Long, () -> Unit) -> Unit,
) {
    checkContact(
        handle,
        email,
        isContact,
        { openContactInfoActivity(context, email) },
        {
            coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    context.getString(
                        R.string.user_is_not_contact,
                        name
                    ),
                    context.getString(R.string.contact_invite),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    inviteUser(
                        email,
                        handle
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.contact_invited),
                                context.getString(R.string.general_ok),
                                SnackbarDuration.Indefinite,
                            )
                        }
                    }
                }
            }
        },
        {
            coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    context.getString(
                        R.string.contact_already_invited,
                        name
                    ),
                    context.getString(R.string.tab_sent_requests),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    openSentRequests(context)
                }
            }
        }
    )
}
