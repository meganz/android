package mega.privacy.android.app.presentation.meeting.chat.view.sheet


import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.ReactionsInfoView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReactionUser
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Bottom sheet for reactions for a message
 * @param selectedReaction the reaction user selected and long pressed
 * @param chatId the chat id
 * @param msgId the message id
 * @param modifier
 * @param onUserClick the callback when a certain user is clicked
 * @param getDetailsInReactionList the function to fill the [UIReactionUser] info
 *
 */
@Composable
fun ReactionsInfoBottomSheet(
    selectedReaction: String,
    reactions: List<UIReaction>,
    uiState: ChatUiState,
    scaffoldState: ScaffoldState,
    getUser: suspend (UserId) -> User?,
    modifier: Modifier = Modifier,
    onUserClicked: () -> Unit = {},
    getDetailsInReactionList: suspend (List<UIReaction>) -> List<UIReaction> = { emptyList() },
) {
    val coroutineScope = rememberCoroutineScope()
    val reactionList = produceState(initialValue = reactions) {
        coroutineScope.launch {
            runCatching {
                value = getDetailsInReactionList(reactions)
            }
        }
    }

    val context = LocalContext.current
    val onUserClick: (Long) -> Unit = { userHandle ->
        coroutineScope.launch {
            val isMe = uiState.myUserHandle == userHandle
            if (isMe) {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(context.getString(R.string.contact_is_me))
            } else {
                getUser(UserId(userHandle))?.let { user ->
                    val isUserMyContact = user.visibility == UserVisibility.Visible
                    if (isUserMyContact) {
                        openContactInfoActivity(context, user.email)
                    }
                }
            }
        }
    }

    ReactionsInfoView(
        modifier = modifier,
        currentReaction = selectedReaction,
        reactionList = reactionList.value,
        onUserClick = {
            onUserClick(it)
            onUserClicked()
        },
    )
}
