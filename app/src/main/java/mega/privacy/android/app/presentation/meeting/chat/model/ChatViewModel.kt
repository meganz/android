package mega.privacy.android.app.presentation.meeting.chat.model

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.main.megachat.MapsActivity
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.mapper.ForwardMessagesResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteParticipantResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.ParticipantNameMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageBottomSheetAction
import mega.privacy.android.app.presentation.meeting.chat.view.actions.MessageAction
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.INVALID_LOCATION_MESSAGE_ID
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.ChatArgs
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.chat.CreateChatException
import mega.privacy.android.domain.exception.chat.ResourceDoesNotExistChatException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.call.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.CloseChatPreviewUseCase
import mega.privacy.android.domain.usecase.chat.EnableGeolocationUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.GetCustomSubtitleListUseCase
import mega.privacy.android.domain.usecase.chat.HoldChatCallUseCase
import mega.privacy.android.domain.usecase.chat.InviteToChatUseCase
import mega.privacy.android.domain.usecase.chat.IsAnonymousModeUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.IsGeolocationEnabledUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatPendingChangesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeavingChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorParticipatingInACallInOtherChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.OpenChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.RecordAudioUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.MonitorJoiningChatUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachContactsUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.GetChatFromContactMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.SendGiphyMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.SendLocationMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.delete.DeleteMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.edit.EditLocationMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.edit.EditMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardMessagesUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.AddReactionUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.DeleteReactionUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import mega.privacy.android.domain.usecase.contact.MonitorAllContactParticipantsInChatUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.contact.MonitorUserLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.CreateNewImageUriUseCase
import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastUpgradeDialogClosedUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.GetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.SetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.shared.original.core.ui.controls.chat.VoiceClipRecordEvent
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReactionUser
import mega.privacy.mobile.analytics.event.ChatConversationUnmuteMenuToolbarEvent
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Extra Action
 */
const val EXTRA_ACTION = "ACTION"

/**
 * Extra Link
 */
const val EXTRA_LINK = "LINK"

/**
 * Chat view model.
 *
 * @property isChatNotificationMuteUseCase
 * @property getChatRoomUseCase
 * @property monitorChatRoomUpdatesUseCase
 * @property monitorUpdatePushNotificationSettingsUseCase
 * @property monitorUserChatStatusByHandleUseCase
 * @property getFeatureFlagValueUseCase
 * @property setUsersCallLimitRemindersUseCase
 * @property getUsersCallLimitRemindersUseCase
 * @property state UI state.
 *
 * @param savedStateHandle
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase,
    private val monitorParticipatingInACallInOtherChatsUseCase: MonitorParticipatingInACallInOtherChatsUseCase,
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase,
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val monitorUserLastGreenUpdatesUseCase: MonitorUserLastGreenUpdatesUseCase,
    private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
    private val monitorHasAnyContactUseCase: MonitorHasAnyContactUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val getCustomSubtitleListUseCase: GetCustomSubtitleListUseCase,
    private val monitorAllContactParticipantsInChatUseCase: MonitorAllContactParticipantsInChatUseCase,
    private val inviteToChatUseCase: InviteToChatUseCase,
    private val inviteParticipantResultMapper: InviteParticipantResultMapper,
    private val unmuteChatNotificationUseCase: UnmuteChatNotificationUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val chatManagement: ChatManagement,
    private val muteChatNotificationForChatRoomsUseCase: MuteChatNotificationForChatRoomsUseCase,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val isGeolocationEnabledUseCase: IsGeolocationEnabledUseCase,
    private val enableGeolocationUseCase: EnableGeolocationUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val holdChatCallUseCase: HoldChatCallUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val isAnonymousModeUseCase: IsAnonymousModeUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val openChatLinkUseCase: OpenChatLinkUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val closeChatPreviewUseCase: CloseChatPreviewUseCase,
    private val createNewImageUriUseCase: CreateNewImageUriUseCase,
    private val monitorJoiningChatUseCase: MonitorJoiningChatUseCase,
    private val monitorLeavingChatUseCase: MonitorLeavingChatUseCase,
    private val sendLocationMessageUseCase: SendLocationMessageUseCase,
    private val monitorChatPendingChangesUseCase: MonitorChatPendingChangesUseCase,
    private val addReactionUseCase: AddReactionUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val deleteReactionUseCase: DeleteReactionUseCase,
    private val sendGiphyMessageUseCase: SendGiphyMessageUseCase,
    private val attachContactsUseCase: AttachContactsUseCase,
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase,
    private val participantNameMapper: ParticipantNameMapper,
    private val getUserUseCase: GetUserUseCase,
    private val forwardMessagesUseCase: ForwardMessagesUseCase,
    private val forwardMessagesResultMapper: ForwardMessagesResultMapper,
    private val attachNodeUseCase: AttachNodeUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val addNodeType: AddNodeType,
    private val deleteMessagesUseCase: DeleteMessagesUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val editLocationMessageUseCase: EditLocationMessageUseCase,
    private val getChatFromContactMessagesUseCase: GetChatFromContactMessagesUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorLeaveChatUseCase: MonitorLeaveChatUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val setUsersCallLimitRemindersUseCase: SetUsersCallLimitRemindersUseCase,
    private val getUsersCallLimitRemindersUseCase: GetUsersCallLimitRemindersUseCase,
    private val broadcastUpgradeDialogClosedUseCase: BroadcastUpgradeDialogClosedUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase,
    actionFactories: Set<@JvmSuppressWildcards (ChatViewModel) -> MessageAction>,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val conversationArgs = ChatArgs(savedStateHandle)
    private val chatId = conversationArgs.chatId
    private val launchAction = conversationArgs.action
    private val chatLink = conversationArgs.link.orEmpty()

    private val usersTyping = Collections.synchronizedMap(mutableMapOf<Long, String?>())
    private val jobs = mutableMapOf<Long, Job>()

    private val actions = actionFactories.map { it(this) }

    private val ChatRoom.haveAtLeastReadPermission: Boolean
        get() = ownPrivilege != ChatRoomPermission.Unknown
                && ownPrivilege != ChatRoomPermission.Removed

    private var monitorAllContactParticipantsInChatJob: Job? = null
    private var monitorChatRoomUpdatesJob: Job? = null
    private var monitorCallInChatJob: Job? = null
    private var monitorParticipatingInACallInOtherChatsJob: Job? = null
    private var monitorChatConnectionStateJob: Job? = null
    private var monitorConnectivityJob: Job? = null

    init {
        getApiFeatureFlag()
        checkUsersCallLimitReminders()
        getMyUserHandle()
        checkGeolocation()
        monitorStorageStateEvent()
        monitorHasAnyContact()
        loadChatOrPreview()
        checkAnonymousMode()
        monitorNotificationMute()
        monitorJoiningChat()
        monitorLeavingChat()
        monitorLeaveChat()
        monitorChatRoomPreference()
    }

    /**
     * Get call unlimited pro plan api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    private fun monitorChatRoomPreference() {
        viewModelScope.launch {
            monitorChatPendingChangesUseCase(chatId)
                .catch { Timber.e(it) }
                .collect { preference ->
                    preference.editingMessageId?.let { editingMessageId ->
                        runCatching {
                            getChatMessageUseCase(chatId, editingMessageId)
                        }.onFailure { Timber.e(it) }
                            .getOrNull()?.takeIf { it.isEditable }
                    }?.let { chatMessage ->
                        _state.update { state ->
                            state.copy(
                                sendingText = preference.draftMessage,
                                editingMessageId = chatMessage.messageId,
                                editingMessageContent = chatMessage.content
                            )
                        }
                    } ?: run {
                        _state.update { state -> state.copy(sendingText = preference.draftMessage) }
                    }
                }
        }
    }

    /**
     * Enable or disable users call limit reminder
     */
    fun setUsersCallLimitReminder(enabled: Boolean) = viewModelScope.launch {
        runCatching {
            setUsersCallLimitRemindersUseCase(if (enabled) UsersCallLimitReminders.Enabled else UsersCallLimitReminders.Disabled)
        }.onFailure { exception ->
            Timber.e("An error occurred when setting the call limit reminder", exception)
        }
    }

    private fun loadChatOrPreview() {
        if (chatLink.isNotEmpty()) {
            val requireJoin = launchAction == Constants.ACTION_JOIN_OPEN_CHAT_LINK
            viewModelScope.launch {
                runCatching {
                    openChatLinkUseCase(
                        chatLink = chatLink,
                        chatId = chatId,
                        requireJoin = requireJoin,
                    )
                }.onSuccess {
                    loadChatRoom()
                }.onFailure {
                    Timber.e(it)
                    val infoToShow = if (it is ResourceDoesNotExistChatException) {
                        InfoToShow.SimpleString(stringId = R.string.invalid_chat_link)
                    } else {
                        InfoToShow.SimpleString(stringId = R.string.error_general_nodes)
                    }
                    _state.update { state -> state.copy(infoToShowEvent = triggered(infoToShow)) }
                }
            }
        } else {
            loadChatRoom()
        }
    }

    private fun checkAnonymousMode() {
        viewModelScope.launch {
            val isAnonymousMode = isAnonymousModeUseCase()
            _state.update { state -> state.copy(isAnonymousMode = isAnonymousMode) }
        }
    }

    private fun loadChatRoom() {
        getChatRoom()
        getNotificationMute()
        getChatConnectionState()
        getScheduledMeeting()
        monitorACallInThisChat()
        monitorParticipatingInACall()
        monitorChatRoom()
        monitorChatConnectionState()
        monitorNetworkConnectivity()
    }

    private fun monitorJoiningChat() {
        viewModelScope.launch {
            monitorJoiningChatUseCase(chatId)
                .collectLatest { isJoining ->
                    if (!isJoining) {
                        loadChatRoom()
                    } else {
                        _state.update { state -> state.copy(isJoining = true) }
                    }
                }
        }
    }

    private fun monitorLeavingChat() {
        viewModelScope.launch {
            monitorLeavingChatUseCase(chatId)
                .collectLatest { _state.update { state -> state.copy(isLeaving = it) } }
        }
    }

    private fun monitorAllContactParticipantsInChat(peerHandles: List<Long>) {
        monitorAllContactParticipantsInChatJob?.cancel()
        monitorAllContactParticipantsInChatJob = viewModelScope.launch {
            monitorAllContactParticipantsInChatUseCase(peerHandles)
                .catch { Timber.e(it) }
                .collect { allContactsParticipateInChat ->
                    _state.update { state -> state.copy(allContactsParticipateInChat = allContactsParticipateInChat) }
                }
        }
    }

    private fun getScheduledMeeting() {
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChatUseCase(chatId)
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.firstOrNull { it.parentSchedId == INVALID_HANDLE }
                    ?.let { meeting ->
                        _state.update {
                            it.copy(
                                schedIsPending = !meeting.isPast(),
                                scheduledMeeting = meeting
                            )
                        }
                    }
            }.onFailure {
                Timber.e(it)
                _state.update { state -> state.copy(scheduledMeeting = null) }
            }
        }
    }

    private fun monitorNetworkConnectivity() {
        monitorConnectivityJob?.cancel()
        monitorConnectivityJob = viewModelScope.launch {
            monitorConnectivityUseCase()
                .collect { networkConnected ->
                    val isChatConnected = if (networkConnected) {
                        isChatStatusConnectedForCallUseCase(chatId = chatId)
                    } else {
                        false
                    }

                    _state.update {
                        it.copy(isConnected = isChatConnected)
                    }
                }
        }
    }

    private fun monitorChatConnectionState() {
        monitorChatConnectionStateJob?.cancel()
        monitorChatConnectionStateJob = viewModelScope.launch {
            monitorChatConnectionStateUseCase()
                .filter { it.chatId == chatId }
                .collect { state ->
                    if (state.chatConnectionStatus != ChatConnectionStatus.Online) {
                        _state.update {
                            it.copy(isConnected = false)
                        }
                    } else {
                        getChatConnectionState()
                    }
                }
        }
    }

    private fun getChatConnectionState() {
        viewModelScope.launch {
            runCatching {
                isChatStatusConnectedForCallUseCase(chatId = chatId)
            }.onSuccess { connected ->
                _state.update { state ->
                    state.copy(isConnected = connected)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorStorageStateEvent() {
        viewModelScope.launch {
            monitorStorageStateEventUseCase()
                .collect { storageState ->
                    _state.update { state -> state.copy(storageState = storageState.storageState) }
                }
        }
    }

    private fun getChatRoom() {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let {
                    with(chatRoom) {
                        checkCustomTitle()
                        val participantsCount = getNumberParticipants()
                        _state.update { state ->
                            val isJoining = false.takeIf { !isPreview } ?: state.isJoining
                            state.copy(
                                chat = chatRoom,
                                isJoining = isJoining,
                                participantsCount = participantsCount,
                            )
                        }
                        if (peerHandlesList.isNotEmpty()) {
                            if (!isGroup) {
                                peerHandlesList[0].let {
                                    getUserChatStatus(it)
                                    monitorUserOnlineStatusUpdates(it)
                                    monitorUserLastGreen(it)
                                }
                            } else {
                                monitorAllContactParticipantsInChat(peerHandlesList)
                            }
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun ChatRoom.checkCustomTitle() {
        if (!isPreview && isGroup && hasCustomTitle) {
            viewModelScope.launch {
                runCatching { getCustomSubtitleListUseCase(chatId, peerHandlesList) }
                    .onSuccess { customSubtitleList ->
                        _state.update { state -> state.copy(customSubtitleList = customSubtitleList) }
                    }
                    .onFailure { Timber.w(it) }
            }
        }
    }

    private fun ChatRoom.getNumberParticipants() =
        (peerCount + if (haveAtLeastReadPermission) 1 else 0)
            .takeIf { isGroup }

    private fun getUserChatStatus(userHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getUserOnlineStatusByHandleUseCase(userHandle)
            }.onSuccess { userChatStatus ->
                updateUserChatStatus(userHandle, userChatStatus)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getNotificationMute() {
        viewModelScope.launch {
            runCatching {
                isChatNotificationMuteUseCase(chatId)
            }.onSuccess { isMute ->
                _state.update { it.copy(isChatNotificationMute = isMute) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorChatRoom() {
        monitorChatRoomUpdatesJob?.cancel()
        monitorChatRoomUpdatesJob = viewModelScope.launch {
            monitorChatRoomUpdatesUseCase(chatId)
                .collect { chat ->
                    with(chat) {
                        _state.update { state -> state.copy(chat = chat) }

                        changes?.forEach { change ->
                            when (change) {
                                ChatRoomChange.Title -> {
                                    checkCustomTitle()
                                }

                                ChatRoomChange.OwnPrivilege,
                                ChatRoomChange.Closed,
                                -> {
                                    _state.update { state ->
                                        state.copy(
                                            participantsCount = getNumberParticipants()
                                        )
                                    }
                                }

                                ChatRoomChange.UserTyping -> {
                                    if (userTyping != state.value.myUserHandle) {
                                        handleUserTyping(userTyping)
                                    }
                                }

                                ChatRoomChange.UserStopTyping -> {
                                    handleUserStopTyping(userTyping)
                                }

                                ChatRoomChange.Participants -> {
                                    checkCustomTitle()
                                    _state.update { state ->
                                        state.copy(participantsCount = getNumberParticipants())
                                    }
                                    monitorAllContactParticipantsInChat(peerHandlesList)
                                }

                                else -> {
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun handleUserStopTyping(userTypingHandle: Long) {
        jobs[userTypingHandle]?.cancel()
        usersTyping.remove(userTypingHandle)
        _state.update { state ->
            state.copy(usersTyping = usersTyping.values.toList())
        }
    }

    private fun handleUserTyping(userTypingHandle: Long) {
        // if user is in the map, we don't need to add again
        if (!usersTyping.contains(userTypingHandle)) {
            viewModelScope.launch {
                val firstName = getParticipantFirstNameUseCase(userTypingHandle)
                usersTyping[userTypingHandle] = firstName
                _state.update { state ->
                    state.copy(usersTyping = usersTyping.values.toList())
                }
            }
        }
        // if user continue typing, cancel timer and start new timer
        jobs[userTypingHandle]?.cancel()
        jobs[userTypingHandle] = viewModelScope.launch {
            delay(TimeUnit.SECONDS.toMillis(5))
            usersTyping.remove(userTypingHandle)
            _state.update { state ->
                state.copy(usersTyping = usersTyping.values.toList())
            }
        }
    }

    private fun monitorNotificationMute() {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect { changed ->
                if (changed) {
                    getNotificationMute()
                }
            }
        }
    }

    private fun monitorUserOnlineStatusUpdates(userHandle: Long) {
        viewModelScope.launch {
            monitorUserChatStatusByHandleUseCase(userHandle).conflate()
                .collect { userChatStatus ->
                    updateUserChatStatus(userHandle, userChatStatus)
                }
        }
    }

    private fun updateUserChatStatus(userHandle: Long, userChatStatus: UserChatStatus) {
        viewModelScope.launch {
            if (userChatStatus != UserChatStatus.Online) {
                _state.update { state -> state.copy(userChatStatus = userChatStatus) }
                runCatching { requestUserLastGreenUseCase(userHandle) }
                    .onFailure { Timber.e(it) }
            } else {
                _state.update { state ->
                    state.copy(
                        userChatStatus = userChatStatus,
                        userLastGreen = null
                    )
                }
            }
        }
    }

    private fun monitorParticipatingInACall() {
        monitorParticipatingInACallInOtherChatsJob?.cancel()
        monitorParticipatingInACallInOtherChatsJob = viewModelScope.launch {
            monitorParticipatingInACallInOtherChatsUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Monitor call in progress returned chat id: $it")
                    _state.update { state -> state.copy(callsInOtherChats = it) }
                }
        }
    }

    private fun monitorACallInThisChat() {
        monitorCallInChatJob?.cancel()
        monitorCallInChatJob = viewModelScope.launch {
            monitorCallInChatUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    it?.apply {
                        when (status) {
                            ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.GenericNotification ->
                                when (termCode) {
                                    ChatCallTermCodeType.TooManyParticipants -> {
                                        val infoToShow =
                                            InfoToShow.SimpleString(stringId = R.string.call_error_too_many_participants)
                                        _state.update { state ->
                                            state.copy(
                                                infoToShowEvent = triggered(
                                                    infoToShow
                                                )
                                            )
                                        }
                                    }

                                    ChatCallTermCodeType.CallDurationLimit -> {
                                        if (it.isOwnClientCaller && _state.value.isCallUnlimitedProPlanFeatureFlagEnabled) {
                                            _state.update { state ->
                                                state.copy(
                                                    shouldUpgradeToProPlan = true
                                                )
                                            }
                                        }
                                    }

                                    ChatCallTermCodeType.CallUsersLimit -> {
                                        if (_state.value.isCallUnlimitedProPlanFeatureFlagEnabled) {
                                            setUsersCallLimitReminder(true)
                                            _state.update { state ->
                                                state.copy(
                                                    callEndedDueToFreePlanLimits = true
                                                )
                                            }
                                        }
                                    }

                                    else -> {}
                                }

                            else -> {}
                        }
                    }
                    _state.update { state -> state.copy(callInThisChat = it) }
                }
        }
    }

    private fun monitorUserLastGreen(userHandle: Long) {
        viewModelScope.launch {
            monitorUserLastGreenUpdatesUseCase(userHandle).conflate()
                .collect { userLastGreen ->
                    if (state.value.userChatStatus != UserChatStatus.Online) {
                        _state.update { state -> state.copy(userLastGreen = userLastGreen) }
                    }
                }
        }
    }

    private fun monitorHasAnyContact() {
        viewModelScope.launch {
            monitorHasAnyContactUseCase().conflate()
                .collect { hasAnyContact ->
                    _state.update { state -> state.copy(hasAnyContact = hasAnyContact) }
                }
        }
    }

    /**
     * Get another call participating
     *
     */
    fun enablePasscodeCheck() {
        passcodeManagement.showPasscodeScreen = true
    }


    /**
     * Handle action press
     *
     * @param action [ChatRoomMenuAction].
     */
    fun handleActionPress(action: ChatRoomMenuAction) {
        when (action) {
            is ChatRoomMenuAction.Unmute -> {
                Analytics.tracker.trackEvent(ChatConversationUnmuteMenuToolbarEvent)
                unmutePushNotification()
            }

            else -> {}
        }
    }

    private fun unmutePushNotification() {
        viewModelScope.launch {
            runCatching {
                unmuteChatNotificationUseCase(chatId)
                _state.update {
                    it.copy(
                        infoToShowEvent = triggered(
                            InfoToShow.MuteOptionResult(result = ChatPushNotificationMuteOption.Unmute)
                        )
                    )
                }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Mute chat push notification based on user selection. And once mute operation succeeds,
     * send [InfoToShow] to show a message to indicate the result in UI.
     *
     * @param option [ChatPushNotificationMuteOption]
     */
    fun mutePushNotification(option: ChatPushNotificationMuteOption) {
        viewModelScope.launch {
            runCatching {
                muteChatNotificationForChatRoomsUseCase(listOf(chatId), option)
                _state.update {
                    it.copy(
                        infoToShowEvent = triggered(
                            InfoToShow.MuteOptionResult(result = option)
                        )
                    )
                }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Invite contacts to chat.
     */
    fun inviteContactsToChat(chatId: Long, contactsData: List<String>) =
        viewModelScope.launch {
            runCatching {
                inviteToChatUseCase(chatId, contactsData)
            }.onSuccess { result ->
                _state.update { state ->
                    state.copy(
                        infoToShowEvent = triggered(
                            InfoToShow.InviteContactResult(
                                result = inviteParticipantResultMapper(result)
                            )
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    /**
     * Clear chat history.
     */
    fun clearChatHistory() {
        viewModelScope.launch {
            runCatching { clearChatHistoryUseCase(chatId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(InfoToShow.SimpleString(stringId = R.string.clear_history_success))
                        )
                    }
                }
                .onFailure {
                    Timber.e("Error clearing chat history $it")
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(InfoToShow.SimpleString(stringId = R.string.clear_history_error))
                        )
                    }
                }
        }
    }

    /**
     * Consumes the event of showing info.
     */
    fun onInfoToShowEventConsumed() {
        _state.update { state -> state.copy(infoToShowEvent = consumed()) }
    }

    /**
     * Ends a call.
     */
    fun endCall() {
        viewModelScope.launch {
            runCatching {
                endCallUseCase(chatId)
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Archive chat.
     */
    fun archiveChat() {
        viewModelScope.launch {
            runCatching { archiveChatUseCase(chatId, true) }
                .onSuccess {
                    broadcastChatArchivedUseCase(_state.value.title.orEmpty())
                    _state.update { state ->
                        state.copy(
                            actionToManageEvent = triggered(
                                ActionToManage.CloseChat
                            )
                        )
                    }
                }.onFailure {
                    Timber.e("Error archiving chat $it")
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                InfoToShow.StringWithParams(
                                    stringId = R.string.error_archive_chat,
                                    args = state.title?.let { title -> listOf(title) }.orEmpty()
                                )
                            )
                        )
                    }
                }
        }
    }

    /**
     * Unarchive chat.
     */
    fun unarchiveChat() {
        viewModelScope.launch {
            runCatching { archiveChatUseCase(chatId, false) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                InfoToShow.StringWithParams(
                                    stringId = R.string.success_unarchive_chat,
                                    args = state.title?.let { title -> listOf(title) }.orEmpty()
                                )
                            )
                        )
                    }
                }
                .onFailure {
                    Timber.e("Error unarchiving chat $it")
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                InfoToShow.StringWithParams(
                                    stringId = R.string.error_unarchive_chat,
                                    args = state.title?.let { title -> listOf(title) }.orEmpty()
                                )
                            )
                        )
                    }
                }
        }
    }

    /**
     * Start a call.
     */
    fun startCall(video: Boolean) {
        setUsersCallLimitReminder(enabled = true)
        viewModelScope.launch {
            runCatching { startCallUseCase(chatId = chatId, audio = true, video = video) }
                .onSuccess { call ->
                    setCallReady(call)
                }.onFailure { Timber.e("Exception starting call $it") }
        }
    }

    private fun setCallReady(call: ChatCall?) {
        call?.let {
            chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
            chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
            passcodeManagement.showPasscodeScreen = true
            _state.update { state ->
                state.copy(callInThisChat = call, isStartingCall = true)
            }
        }
    }

    /**
     * On call started.
     */
    fun onCallStarted() {
        _state.update { state -> state.copy(isStartingCall = false) }
    }

    /**
     * On opened waiting room
     */
    fun onWaitingRoomOpened() {
        _state.update { state -> state.copy(openWaitingRoomScreen = false) }
    }

    /**
     * Start or join a meeting.
     */
    fun onStartOrJoinMeeting(isStarted: Boolean) {
        setUsersCallLimitReminder(enabled = true)
        val isWaitingRoom = state.value.isWaitingRoom
        if (isStarted) {
            val isHost = state.value.myPermission == ChatRoomPermission.Moderator
            if (isWaitingRoom && !isHost) {
                _state.update { state -> state.copy(openWaitingRoomScreen = true) }
            } else {
                onAnswerCall()
            }
        } else {
            if (isWaitingRoom) {
                startWaitingRoomMeeting()
            } else {
                startMeeting()
            }
        }
    }

    private fun startWaitingRoomMeeting() {
        setUsersCallLimitReminder(enabled = true)
        val isHost = state.value.myPermission == ChatRoomPermission.Moderator
        if (isHost) {
            viewModelScope.launch {
                runCatching {
                    val schedId = state.value.scheduledMeeting?.schedId ?: -1L
                    startMeetingInWaitingRoomChatUseCase(
                        chatId = chatId,
                        schedIdWr = schedId,
                        enabledVideo = false,
                        enabledAudio = true,
                    )
                }.onSuccess { chatCall ->
                    setCallReady(chatCall)
                }.onFailure {
                    Timber.e(it)
                }
            }
        } else {
            _state.update { state -> state.copy(openWaitingRoomScreen = true) }
        }
    }

    private fun startMeeting() {
        setUsersCallLimitReminder(enabled = true)
        viewModelScope.launch {
            runCatching {
                val scheduledMeeting = requireNotNull(state.value.scheduledMeeting)
                startChatCallNoRingingUseCase(
                    chatId = chatId,
                    schedId = scheduledMeeting.schedId,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { chatCall ->
                setCallReady(chatCall)
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Answers a call.
     */
    fun onAnswerCall() {
        setUsersCallLimitReminder(enabled = true)
        viewModelScope.launch {
            chatManagement.addJoiningCallChatId(chatId)
            runCatching {
                answerChatCallUseCase(chatId = chatId, video = false, audio = true)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    _state.update { state -> state.copy(isStartingCall = true) }
                }
            }.onFailure {
                Timber.w("Exception answering call: $it")
                chatManagement.removeJoiningCallChatId(chatId)
            }
        }
    }

    private fun checkGeolocation() {
        viewModelScope.launch {
            runCatching { isGeolocationEnabledUseCase() }
                .onSuccess { isGeolocationEnabled ->
                    _state.update { state -> state.copy(isGeolocationEnabled = isGeolocationEnabled) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Enables geolocation.
     */
    fun onEnableGeolocation() {
        viewModelScope.launch {
            runCatching { enableGeolocationUseCase() }
                .onSuccess {
                    _state.update { state -> state.copy(isGeolocationEnabled = true) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Sends a text message.
     */
    fun sendTextMessage(message: String) {
        viewModelScope.launch {
            state.value.editingMessageId?.let {
                val editedMessage = editMessageUseCase(chatId, it, message)
                onCloseEditing()
                if (editedMessage == null) {
                    messageCannotBeEdited()
                }
            } ?: sendTextMessageUseCase(chatId, message)
        }
    }

    /**
     * Holds an existing in progress call and answers a new one.
     */
    fun onHoldAndAnswerCall() {
        viewModelScope.launch {
            val callToHold = state.value.callsInOtherChats
                .find { it.status?.isJoined == true }

            callToHold?.chatId?.let {
                if (callToHold.isOnHold) {
                    // The call is already on hold, just answer
                    onAnswerCall()
                } else {
                    runCatching {
                        holdChatCallUseCase(chatId = it, setOnHold = true)
                    }.onFailure { Timber.e(it) }
                        .onSuccess { onAnswerCall() }
                }
            } ?: run {
                // The call finished before setting on hold, just answer
                onAnswerCall()
            }
        }
    }

    /**
     * Ends an existing in progress call and answers a new one.
     */
    fun onEndAndAnswerCall() {
        viewModelScope.launch {
            state.value.callsInOtherChats
                .find { it.status?.isJoined == true && !it.isOnHold }?.callId?.let {
                    runCatching {
                        hangChatCallUseCase(it)
                    }.onFailure { Timber.e(it) }
                        .onSuccess { onAnswerCall() }
                } ?: run {
                // The call finished before ending, just answer
                onAnswerCall()
            }
        }
    }


    /**
     * Sets pending link to join.
     */
    fun onSetPendingJoinLink() {
        chatManagement.pendingJoinLink = chatLink
    }

    /**
     * Attaches files.
     */
    fun onAttachFiles(files: List<Uri>) {
        _state.update {
            it.copy(
                downloadEvent = triggered(
                    TransferTriggerEvent.StartChatUpload.Files(chatId, files)
                )
            )
        }
    }

    /**
     * Attaches voice clip.
     */
    private fun onAttachVoiceClip(file: File) {
        _state.update {
            it.copy(
                downloadEvent = triggered(
                    TransferTriggerEvent.StartChatUpload.VoiceClip(chatId, file)
                )
            )
        }
    }

    /**
     * Attach nodes
     *
     * @param nodes A list of all [NodeId] that will be attached. It should refer to [FileNode]s only, other node types will be discarded.
     */
    fun onAttachNodes(nodes: List<NodeId>) {
        viewModelScope.launch {
            val sent = nodes
                .mapNotNull { runCatching { getNodeByIdUseCase(it) }.getOrNull() }
                .filterIsInstance<FileNode>()
                .map {
                    runCatching {
                        attachNodeUseCase(chatId, addNodeType(it) as TypedFileNode)
                    }.isSuccess
                }.count { it }
            if (sent != nodes.size) {
                _state.update { state ->
                    state.copy(infoToShowEvent = triggered(InfoToShow.SimpleString(R.string.files_send_to_chat_error)))
                }
            }
        }
    }

    /**
     * Joins a public chat.
     */
    fun onJoinChat() {
        viewModelScope.launch {
            runCatching {
                joinPublicChatUseCase(chatId)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Creates a new image uri.
     */
    suspend fun createNewImageUri(): Uri? {
        Timber.d("createNewImageUri")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "picture${timeStamp}.jpg"
        return createNewImageUriUseCase(imageFileName)?.let { Uri.parse(it) }
    }

    /**
     * Send location message.
     */
    fun sendLocationMessage(data: Intent?) {
        data?.let {
            val byteArray = data.getByteArrayExtra(MapsActivity.SNAPSHOT) ?: return
            val latitude = data.getDoubleExtra(MapsActivity.LATITUDE, 0.0).toFloat()
            val longitude = data.getDoubleExtra(MapsActivity.LONGITUDE, 0.0).toFloat()
            val isEditing = data.getBooleanExtra(MapsActivity.EDITING_MESSAGE, false)
            val msgId = data.getLongExtra(MapsActivity.MSG_ID, INVALID_LOCATION_MESSAGE_ID)

            viewModelScope.launch {
                val encodedSnapshot = Base64.encodeToString(byteArray, Base64.DEFAULT)
                if (isEditing) {
                    val editedMessage = editLocationMessageUseCase(
                        chatId = chatId,
                        msgId = msgId,
                        longitude = longitude,
                        latitude = latitude,
                        image = encodedSnapshot
                    )
                    if (editedMessage == null) {
                        messageCannotBeEdited()
                    }
                } else {
                    sendLocationMessageUseCase(
                        chatId = chatId,
                        latitude = latitude,
                        longitude = longitude,
                        image = encodedSnapshot
                    )
                }
            }
        }
    }

    /**
     * On close editing
     *
     */
    fun onCloseEditing() {
        _state.update { state ->
            state.copy(
                editingMessageId = null,
                editingMessageContent = null,
                sendingText = "",
            )
        }
    }

    /**
     * Add reaction to a message.
     *
     * @param msgId The message id.
     * @param reaction The reaction to add.
     */
    fun onAddReaction(msgId: Long, reaction: String) {
        viewModelScope.launch {
            runCatching { addReactionUseCase(chatId, msgId, reaction) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Delete reaction in a message.
     *
     * @param msgId The message id.
     * @param reaction The reaction to remove.
     */
    fun onDeleteReaction(msgId: Long, reaction: String) {
        viewModelScope.launch {
            runCatching { deleteReactionUseCase(chatId, msgId, reaction) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Send giphy message.
     */
    fun onSendGiphyMessage(gifData: GifData?) {
        gifData?.let {
            with(gifData) {
                viewModelScope.launch {
                    sendGiphyMessageUseCase(
                        chatId = chatId,
                        srcMp4 = mp4Url,
                        srcWebp = webpUrl,
                        sizeMp4 = mp4Size,
                        sizeWebp = webpSize,
                        width = width,
                        height = height,
                        title = title
                    )
                }
            }
        }
    }

    /**
     * Attach one or more contacts to the chat.
     */
    fun onAttachContacts(contacts: List<String>) {
        viewModelScope.launch {
            attachContactsUseCase(chatId, contacts)
        }
    }

    /**
     * Fill the user name in the [UIReactionUser] of [UIReaction]
     *
     * @param reactions list of [UIReaction]
     * @return another list of [UIReaction] in which user info has been filled.
     */
    suspend fun getUserInfoIntoReactionList(reactions: List<UIReaction>): List<UIReaction> {
        return reactions.map { reaction ->
            reaction.copy(
                userList = reaction.userList.map { user ->
                    user.copy(
                        name = participantNameMapper(
                            isMe = user.userHandle == state.value.myUserHandle,
                            fullName = getParticipantFullNameUseCase(user.userHandle).orEmpty()
                        ),
                    )
                }
            )
        }
    }

    /**
     * load my user handle and save to ui state
     */
    private fun getMyUserHandle() {
        viewModelScope.launch {
            runCatching {
                val myUserHandle = getMyUserHandleUseCase()
                _state.update { state -> state.copy(myUserHandle = myUserHandle) }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Get [User] by the [UserId].
     *
     * @param userId
     * @return [User]
     */
    suspend fun getUser(userId: UserId) =
        getUserUseCase(userId)

    /**
     * Forward messages.
     */
    fun onForwardMessages(
        messages: Set<TypedMessage>,
        chatHandles: List<Long>?,
        contactHandles: List<Long>?,
    ) {
        viewModelScope.launch {
            runCatching { forwardMessagesUseCase(messages.toList(), chatHandles, contactHandles) }
                .onSuccess { results ->
                    val result = forwardMessagesResultMapper(results, messages.size)
                    val infoToShow = InfoToShow.ForwardMessagesResult(result)
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(infoToShow)
                        )
                    }
                }
                .onFailure {
                    when (it) {
                        is CreateChatException -> {
                            _state.update { state ->
                                state.copy(
                                    infoToShowEvent = triggered(
                                        InfoToShow.QuantityString(
                                            stringId = R.plurals.num_messages_not_send,
                                            count = contactHandles?.size ?: 0
                                        )
                                    )
                                )
                            }
                        }

                        else -> {
                            Timber.e(it)
                        }
                    }
                }
        }
    }

    /**
     * Delete messages.
     *
     * @param messages list of [TypedMessage].
     */
    fun onDeletedMessages(messages: Set<TypedMessage>) {
        viewModelScope.launch {
            runCatching {
                deleteMessagesUseCase(messages.toList())
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * called when viewmodel is cleared
     */
    override fun onCleared() {
        if (state.value.isPreviewMode) {
            applicationScope.launch {
                runCatching {
                    closeChatPreviewUseCase(chatId)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
        super.onCleared()
    }

    /**
     * Check users call limit reminders
     */
    private fun checkUsersCallLimitReminders() {
        viewModelScope.launch {
            getUsersCallLimitRemindersUseCase().collectLatest { result ->
                _state.update { it.copy(usersCallLimitReminders = result) }
            }
        }
    }

    /**
     * Consume show free plan participants limit dialog event
     *
     */
    fun consumeShowFreePlanParticipantsLimitDialogEvent() {
        setUsersCallLimitReminder(enabled = false)
        _state.update { state -> state.copy(callEndedDueToFreePlanLimits = false) }
    }

    /**
     * Consume download event
     *
     */
    fun consumeDownloadEvent() {
        _state.update { state -> state.copy(downloadEvent = consumed()) }
    }

    /**
     * On download node for preview
     *
     * @param file [ChatFile]
     */
    fun onDownloadForPreviewChatNode(file: ChatFile) {
        _state.update {
            it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForPreview(file)))
        }
    }

    /**
     * On download for offline chat node
     *
     * @param file [ChatFile]
     */
    fun onDownloadForOfflineChatNode(file: ChatFile) {
        _state.update {
            it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForOffline(file)))
        }
    }

    /**
     * On download node
     *
     * @param nodes
     */
    fun onDownloadNode(nodes: List<ChatFile>) {
        _state.update {
            it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadNode(nodes)))
        }
    }

    /**
     * Edit message.
     *
     * @param message [TypedMessage].
     */
    fun onEditMessage(message: TypedMessage) {
        val content = message.content
        if (content.isNullOrEmpty()) {
            onCloseEditing()
            messageCannotBeEdited()
        } else {
            _state.update { state ->
                state.copy(
                    sendingText = content,
                    editingMessageId = message.msgId,
                    editingMessageContent = content,
                )
            }
        }
    }

    /**
     * Send message
     */
    fun onOpenChatWith(messages: List<ContactAttachmentMessage>) {
        viewModelScope.launch {
            runCatching {
                getChatFromContactMessagesUseCase(messages)
            }.onSuccess {
                _state.update { state ->
                    state.copy(actionToManageEvent = triggered(ActionToManage.OpenChat(it)))
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Open chat conversation event consumed
     */
    fun onActionToManageEventConsumed() {
        _state.update { state -> state.copy(actionToManageEvent = consumed()) }
    }

    private fun messageCannotBeEdited() {
        val infoToShow = InfoToShow.SimpleString(R.string.error_editing_message)
        _state.update { state -> state.copy(infoToShowEvent = triggered(infoToShow)) }
    }

    /**
     * Enable select mode.
     */
    fun onEnableSelectMode() {
        _state.update { state ->
            state.copy(actionToManageEvent = triggered(ActionToManage.EnableSelectMode))
        }
    }

    /**
     * Open contact info.
     */
    fun onOpenContactInfo(contactEmail: String) {
        _state.update { state ->
            state.copy(actionToManageEvent = triggered(ActionToManage.OpenContactInfo(contactEmail)))
        }
    }

    /**
     * Handle voice clip record event
     */
    fun onVoiceClipRecordEvent(voiceClipRecordEvent: VoiceClipRecordEvent) {
        when (voiceClipRecordEvent) {
            VoiceClipRecordEvent.Start -> startRecordingVoiceClip()
            VoiceClipRecordEvent.Cancel -> recordAudioJob?.cancel(CancelVoiceClip)
            VoiceClipRecordEvent.Finish -> recordAudioJob?.cancel(StopAndSendVoiceClip)
            VoiceClipRecordEvent.None, VoiceClipRecordEvent.Lock -> {} //nothing here
        }
    }

    private var recordAudioJob: Job? = null
    private fun startRecordingVoiceClip() {
        recordAudioJob?.cancel()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        getCacheFileUseCase(CacheFolderManager.VOICE_CLIP_FOLDER, "note_voice${timeStamp}.m4a")
            ?.let { voiceClipFile ->
                recordAudioJob = viewModelScope.launch {
                    recordAudioUseCase(voiceClipFile).collectLatest {
                        //this should be used to play waveform animation in AND-18251
                        Timber.d("recording audio with amplitude $it")
                    }
                }
                recordAudioJob?.invokeOnCompletion {
                    if (it == StopAndSendVoiceClip) {
                        onAttachVoiceClip(voiceClipFile)
                    } else {
                        viewModelScope.launch {
                            deleteFileUseCase(voiceClipFile.toString())
                        }
                    }
                    if (it !is CancellationException) {
                        Timber.e("Error recording voice clip", it)
                    }
                }
            } ?: run {
            Timber.e("Cache file for voice clip recording can't be created")
        }
    }

    private fun monitorLeaveChat() {
        viewModelScope.launch {
            monitorLeaveChatUseCase()
                .collect { requestChatId ->
                    if (chatId == requestChatId) {
                        leaveChat()
                    }
                }
        }
    }

    private fun leaveChat() {
        viewModelScope.launch {
            runCatching {
                leaveChatUseCase(chatId)
            }.onFailure { exception ->
                if (exception is MegaException) {
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(InfoToShow.SimpleString(exception.getErrorStringId()))
                        )
                    }
                }
            }
        }
    }

    /**
     * Consume ShouldUpgradeToProPlan
     *
     */
    fun onConsumeShouldUpgradeToProPlan() {
        _state.update { state -> state.copy(shouldUpgradeToProPlan = false) }
        viewModelScope.launch {
            runCatching {
                broadcastUpgradeDialogClosedUseCase()
            }
        }
    }

    fun selectMessages(messages: Set<TypedMessage>) {
        _state.update { state -> state.copy(selectedMessages = messages) }
    }

    fun setSelectMode(enabled: Boolean) {
        _state.update { state ->
            if (!enabled) {
                state.copy(
                    selectedMessages = emptySet(),
                    isSelectMode = false
                )
            } else {
                state.copy(isSelectMode = true)
            }
        }
    }

    fun setPendingAction(action: (@Composable () -> Unit)?) {
        _state.update { state -> state.copy(pendingAction = action) }
    }

    fun setSelectedReaction(reaction: String) {
        _state.update { state -> state.copy(selectedReaction = reaction) }
    }

    fun setReactionList(reactions: List<UIReaction>) {
        _state.update { state -> state.copy(reactionList = reactions) }
    }

    fun setAddingReactionTo(id: Long?) {
        _state.update { state -> state.copy(addingReactionTo = id) }
    }

    fun getApplicableBotomsheetActions(hideBottomSheet: () -> Unit): List<MessageBottomSheetAction> =
        getApplicableActions().map { action ->
            MessageBottomSheetAction(
                action.bottomSheetMenuItem(
                    messages = _state.value.selectedMessages,
                    hideBottomSheet = hideBottomSheet,
                    setAction = this::setPendingAction
                ), action.group
            )
        }

    fun getApplicableActions(): List<MessageAction> =
        if (_state.value.selectedMessages.isEmpty()) emptyList()
        else actions.filter { action ->
            action.appliesTo(_state.value.selectedMessages)
        }

    /**
     * Checks if transfers are paused.
     */
    fun areTransfersPaused() = areTransfersPausedUseCase()

    /**
     * Pause transfers.
     */
    fun resumeTransfers() {
        viewModelScope.launch {
            runCatching { pauseTransfersQueueUseCase(false) }
                .onFailure { Timber.e(it) }
        }
    }

    companion object {
        private const val INVALID_HANDLE = -1L
    }
}

internal object StopAndSendVoiceClip : CancellationException()
internal object CancelVoiceClip : CancellationException()
