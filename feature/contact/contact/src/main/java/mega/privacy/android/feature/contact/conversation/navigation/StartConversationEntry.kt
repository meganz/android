package mega.privacy.android.feature.contact.conversation.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.conversation.NoteToSelfViewModel
import mega.privacy.android.feature.contact.conversation.view.StartConversationScreen
import mega.privacy.android.feature.contact.group.create.CreateChatViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.NewChatNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

/**
 * Start conversation entry, rendered for [NewChatNavKey] when it is opened from the chat list.
 *
 * Tapping a contact publishes a 1:1 [NewChatNavKey.NewChatResult] under [NewChatNavKey.KEY]. A group
 * created through [CreateGroupChatNavKey] is forwarded under the same key with its group settings,
 * so the chat list creates and opens either kind of chat. The note-to-self chat already exists, so
 * it is opened directly in place of this screen.
 *
 * @param navigationHandler
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun StartConversationEntry(
    navigationHandler: NavigationHandler,
) {
    val createChatViewModel: CreateChatViewModel = hiltViewModel<CreateChatViewModel>()
    val noteToSelfViewModel: NoteToSelfViewModel = hiltViewModel<NoteToSelfViewModel>()
    val contactsState by createChatViewModel.uiState.collectAsStateWithLifecycle()
    val noteToSelfState by noteToSelfViewModel.uiState.collectAsStateWithLifecycle()

    val newGroupChatResult by navigationHandler
        .monitorResult<Any?>(CreateGroupChatNavKey.KEY)
        .collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(newGroupChatResult) {
        (newGroupChatResult as? CreateGroupChatNavKey.NewGroupChatResult)?.let { result ->
            navigationHandler.clearResult(CreateGroupChatNavKey.KEY)
            navigationHandler.returnResult(NewChatNavKey.KEY, result.toNewChatResult())
        }
    }

    StartConversationScreen(
        contactsState = contactsState,
        noteToSelfState = noteToSelfState,
        onSearchQueryChange = createChatViewModel::setQuery,
        onContactClick = { handle ->
            navigationHandler.returnResult(
                NewChatNavKey.KEY,
                NewChatNavKey.NewChatResult(
                    emails = createChatViewModel.emailsForSelected(setOf(handle)),
                    groupSettings = null,
                ),
            )
        },
        onNewGroupChatClick = {
            navigationHandler.navigate(CreateGroupChatNavKey(allowEmptyGroup = true))
        },
        onInviteClick = {
            navigationHandler.navigate(InviteContactNavKey())
            navigationHandler.remove(NewChatNavKey)
        },
        onNoteToSelfClick = { chatId ->
            navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
            navigationHandler.remove(NewChatNavKey)
        },
        onBack = { navigationHandler.remove(NewChatNavKey) },
    )
}

internal fun CreateGroupChatNavKey.NewGroupChatResult.toNewChatResult() =
    NewChatNavKey.NewChatResult(
        emails = emails,
        groupSettings = NewChatNavKey.NewChatResult.GroupSettings(
            title = title,
            isEkr = isEkr,
            isChatLink = isChatLink,
            allowAddParticipants = allowAddParticipants,
            imageUri = imageUri,
        ),
    )
