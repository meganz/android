package mega.privacy.android.feature.contact.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.contact.group.navigation.ContactGroupsNavKey
import mega.privacy.android.feature.contact.list.ContactListViewModel
import mega.privacy.android.feature.contact.list.view.ContactListScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

/**
 * Contacts entry
 *
 * @param navigationHandler
 * @param viewModel
 */
@Composable
fun ContactsEntry(
    navigationHandler: NavigationHandler,
    viewModel: ContactListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ContactListScreen(
        state = state,
        onSearchQueryChange = viewModel::setQuery,
        onContactClick = viewModel::getChatRoomId,
        onContactInfoClick = { email ->
            navigationHandler.navigate(ContactInfoNavKey(email))
        },
        onAddContactClick = {
            navigationHandler.navigate(InviteContactNavKey())
        },
        onRequestsClick = {
            navigationHandler.navigate(
                ContactRequestsNavKey(ContactRequestsNavKey.NavType.ReceivedRequests)
            )
        },
        onGroupsClick = {
            navigationHandler.navigate(ContactGroupsNavKey)
        },
        onStartCall = viewModel::onCallTap,
        onRemoveContact = viewModel::removeContact,
        onChatEventConsumed = viewModel::onChatEventConsumed,
        onCallEventConsumed = viewModel::onCallEventConsumed,
        onNavigateToChat = { chatId ->
            navigationHandler.navigate(
                ShowChatMessagesNavKey(chatId)
            )
        },
        onStartCallTriggered = { data ->
            navigationHandler.navigate(
                LegacyMeetingNavKey(
                    chatId = data.chatId,
                    meetingInfo = if (data.isExistingCall) {
                        MeetingNavKeyInfo.ReturnToInProgressCall(isGuest = false)
                    } else {
                        MeetingNavKeyInfo.StartOutgoingCall(
                            isAudioEnable = data.hasLocalAudio,
                            isVideoEnable = data.hasLocalVideo,
                        )
                    },
                )
            )
        },
    )
}