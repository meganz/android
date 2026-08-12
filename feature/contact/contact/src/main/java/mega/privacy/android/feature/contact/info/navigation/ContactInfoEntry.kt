package mega.privacy.android.feature.contact.info.navigation

import android.Manifest
import android.annotation.SuppressLint
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
import mega.privacy.android.feature.contact.info.ContactInfoViewModel
import mega.privacy.android.feature.contact.info.model.ContactInfoUiState
import mega.privacy.android.feature.contact.info.view.ContactInfoScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AuthenticityCredentialsNavKey
import mega.privacy.android.navigation.destination.ContactSharedFoldersNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.ManageChatHistoryNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.NodeAttachmentHistoryNavKey
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarningNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

/**
 * Contact info entry. Renders the Compose contact info screen for the contact resolved either
 * from [email] (contact list entry point) or from [chatId] (1:1 chat entry point). Pops itself
 * when the contact cannot be resolved.
 *
 * Hosted by the app module's gated `ContactInfoNavKey` destination (behind `ContactComposeFeature`).
 *
 * @param navigationHandler
 * @param email email of the contact, or null when entering from a chat.
 * @param chatId id of the 1:1 chat with the contact, or null when entering by email.
 * @param onSendFileToChat invoked with the contact's email when the send file toolbar action is
 * selected; the host launches the file selection and attaches the picked files to the chat.
 * @param onShareContact invoked with the contact's email and handle when the share contact row
 * is selected; the host launches the chat selection and attaches the contact to the picked chats.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun ContactInfoEntry(
    navigationHandler: NavigationHandler,
    email: String?,
    chatId: Long?,
    onSendFileToChat: (contactEmail: String) -> Unit,
    onShareContact: (contactEmail: String, userHandle: Long) -> Unit,
) {
    val viewModel = hiltViewModel<ContactInfoViewModel, ContactInfoViewModel.Factory> { factory ->
        factory.create(email = email, chatId = chatId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EventEffect(
        event = state.closeEvent,
        onConsumed = viewModel::onCloseEventConsumed,
    ) {
        navigationHandler.back()
    }
    var startVideoCall by rememberSaveable { mutableStateOf(false) }
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            // A denied camera permission downgrades the call to audio only, mirroring the
            // legacy behaviour.
            viewModel.startCall(
                withVideo = startVideoCall && permissions[Manifest.permission.CAMERA] == true,
            )
        } else {
            viewModel.onCallPermissionsDenied()
        }
    }
    val dataState = state as? ContactInfoUiState.Data
    if (dataState != null) {
        EventEffect(
            event = dataState.openChatEvent,
            onConsumed = viewModel::onOpenChatEventConsumed,
        ) { openChatId ->
            navigationHandler.navigate(ShowChatMessagesNavKey(openChatId))
        }
        EventEffect(
            event = dataState.startCallEvent,
            onConsumed = viewModel::onStartCallEventConsumed,
        ) { callData ->
            navigationHandler.navigate(
                LegacyMeetingNavKey(
                    chatId = callData.chatId,
                    meetingInfo = if (callData.isExistingCall) {
                        MeetingNavKeyInfo.ReturnToInProgressCall(isGuest = false)
                    } else {
                        MeetingNavKeyInfo.StartOutgoingCall(
                            isAudioEnable = callData.hasLocalAudio,
                            isVideoEnable = callData.hasLocalVideo,
                        )
                    },
                )
            )
        }
        EventEffect(
            event = dataState.storageOverQuotaEvent,
            onConsumed = viewModel::onStorageOverQuotaEventConsumed,
        ) {
            navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
        }
    }
    ContactInfoScreen(
        state = state,
        onNavigateBack = navigationHandler::back,
        onSendMessageClick = viewModel::sendMessage,
        onStartAudioCallClick = {
            startVideoCall = false
            callPermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        },
        onStartVideoCallClick = {
            startVideoCall = true
            callPermissionsLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            )
        },
        onUpdateNickname = viewModel::updateNickname,
        onVerifyCredentialsClick = {
            dataState?.email?.let { contactEmail ->
                navigationHandler.navigate(
                    AuthenticityCredentialsNavKey(
                        email = contactEmail,
                        isIncomingShares = false,
                    )
                )
            }
        },
        onShareContactClick = {
            dataState?.let { data ->
                data.email?.let { contactEmail -> onShareContact(contactEmail, data.userHandle) }
            }
        },
        onSharedFoldersClick = {
            dataState?.email?.let { contactEmail ->
                navigationHandler.navigate(ContactSharedFoldersNavKey(contactEmail))
            }
        },
        onNotificationToggled = { viewModel.onNotificationsToggled() },
        onMuteOptionSelected = viewModel::onMuteOptionSelected,
        onMuteOptionsEventConsumed = viewModel::onMuteOptionsEventConsumed,
        onSharedFilesClick = {
            dataState?.chatRoomId?.let { sharedFilesChatId ->
                navigationHandler.navigate(NodeAttachmentHistoryNavKey(sharedFilesChatId))
            }
        },
        onManageChatHistoryClick = {
            dataState?.chatRoomId?.let { historyChatId ->
                navigationHandler.navigate(
                    ManageChatHistoryNavKey(chatId = historyChatId, email = dataState.email)
                )
            }
        },
        onRemoveContact = viewModel::removeContact,
        onMessageEventConsumed = viewModel::onMessageEventConsumed,
        onSendFileClick = {
            dataState?.email?.let(onSendFileToChat)
        },
    )
}
