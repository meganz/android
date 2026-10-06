package mega.privacy.android.feature.contact.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.feature.contact.group.navigation.ContactGroupsNavKey
import mega.privacy.android.feature.contact.list.ContactListViewModel
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import mega.privacy.android.feature.contact.list.view.ContactListScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarningNavKey
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
    var pendingCallHandle by rememberSaveable { mutableStateOf<Long?>(null) }
    var startVideoCall by rememberSaveable { mutableStateOf(false) }
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val userHandle = pendingCallHandle ?: return@rememberLauncherForActivityResult
        pendingCallHandle = null
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            // A denied camera permission downgrades the call to audio only, mirroring the
            // legacy behaviour.
            viewModel.onCallTap(
                userHandle = userHandle,
                video = startVideoCall && permissions[Manifest.permission.CAMERA] == true,
                audio = true,
            )
        } else {
            viewModel.onCallPermissionsDenied()
        }
    }
    (state as? ContactListUiState.Data)?.let { dataState ->
        EventEffect(
            event = dataState.storageOverQuotaEvent,
            onConsumed = viewModel::onStorageOverQuotaEventConsumed,
        ) {
            navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
        }
    }
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
        onStartCall = { userHandle, video ->
            pendingCallHandle = userHandle
            startVideoCall = video
            callPermissionsLauncher.launch(
                if (video) {
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                } else {
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                }
            )
        },
        onRemoveContact = viewModel::removeContact,
        onChatEventConsumed = viewModel::onChatEventConsumed,
        onCallEventConsumed = viewModel::onCallEventConsumed,
        onMessageEventConsumed = viewModel::onMessageEventConsumed,
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
