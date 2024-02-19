package mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import mega.privacy.android.core.ui.controls.chat.messages.ContactAttachmentMessageView as CoreContactAttachmentMessageView
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.extension.toUiChatStatus
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openSentRequests
import mega.privacy.android.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Contact attachment message view
 *
 * @param message Contact attachment message
 * @param modifier Modifier
 * @param viewModel Contact attachment message view model
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactAttachmentMessageView(
    message: ContactAttachmentMessage,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactMessageViewModel = hiltViewModel(),
) {
    var status by remember { mutableStateOf<UserChatStatus?>(null) }
    var userName by remember { mutableStateOf(message.contactUserName) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current

    LaunchedEffect(message.contactEmail) {
        val item = viewModel.loadContactInfo(
            contactEmail = message.contactEmail
        )
        status = item?.status?.takeIf { item.visibility == UserVisibility.Visible }
        userName =
            item?.contactData?.alias ?: item?.contactData?.fullName ?: message.contactUserName
    }
    CoreContactAttachmentMessageView(
        modifier = modifier.combinedClickable(
            onClick = {
                onUserClick(
                    message.contactHandle,
                    message.contactEmail,
                    message.contactUserName,
                    context,
                    coroutineScope,
                    snackbarHostState,
                    viewModel::checkUser,
                    viewModel::inviteUser,
                )
            },
            onLongClick = { onLongClick() }
        ),
        isMe = message.isMine,
        userName = userName,
        email = message.contactEmail,
        status = status.toUiChatStatus(),
        avatar = {
            ChatAvatar(handle = message.contactHandle, modifier = Modifier.size(40.dp))
        },
    )
}

internal fun onUserClick(
    handle: Long,
    email: String,
    name: String,
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState?,
    checkContact: (Long, String, (String) -> Unit, () -> Unit, () -> Unit) -> Unit,
    inviteUser: (String, Long, () -> Unit) -> Unit,
) {
    checkContact(
        handle,
        email,
        { contactEmail ->
            openContactInfoActivity(context, contactEmail)
        },
        {
            coroutineScope.launch {
                val result = snackbarHostState?.showSnackbar(
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
                val result = snackbarHostState?.showSnackbar(
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