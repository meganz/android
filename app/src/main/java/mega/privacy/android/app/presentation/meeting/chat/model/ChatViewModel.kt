package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.HasACallInThisChatByChatIdUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorUserLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat view model.
 *
 * @property isChatNotificationMuteUseCase
 * @property getChatRoomUseCase
 * @property monitorChatRoomUpdates
 * @property monitorUpdatePushNotificationSettingsUseCase
 * @property monitorUserChatStatusByHandleUseCase
 * @property state UI state.
 *
 * @param savedStateHandle
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase,
    private val getChatRoomUseCase: GetChatRoom,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val hasACallInThisChatByChatIdUseCase: HasACallInThisChatByChatIdUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val monitorUserLastGreenUpdatesUseCase: MonitorUserLastGreenUpdatesUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val chatId = savedStateHandle.get<Long>(Constants.CHAT_ID)

    private val ChatRoom.isPrivateRoom: Boolean
        get() = !isGroup || !isPublic

    init {
        checkIfIsParticipatingInACall()
        monitorStorageStateEvent()
        chatId?.let {
            getChatRoom(it)
            getNotificationMute(it)
            checkIfIsParticipatingInACallInThisChat(it)
            monitorChatRoom(it)
            monitorNotificationMute(it)
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

    private fun getChatRoom(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let {
                    with(chatRoom) {
                        _state.update { state ->
                            state.copy(
                                title = title,
                                isPrivateChat = chatRoom.isPrivateRoom,
                                myPermission = ownPrivilege,
                                isPreviewMode = isPreview,
                                isGroup = isGroup,
                                isOpenInvite = isOpenInvite,
                                isActive = isActive,
                                isArchived = isArchived,
                            )
                        }
                        if (!isGroup && peerHandlesList.isNotEmpty()) {
                            peerHandlesList[0].let {
                                getUserChatStatus(it)
                                monitorUserOnlineStatusUpdates(it)
                                monitorUserLastGreen(it)
                            }
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

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

    private fun getNotificationMute(chatId: Long) {
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

    private fun monitorChatRoom(chatId: Long) {
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId)
                .collect { chat ->
                    with(chat) {
                        changes?.forEach { change ->
                            when (change) {
                                ChatRoomChange.Title -> _state.update { state ->
                                    state.copy(title = title)
                                }

                                ChatRoomChange.ChatMode -> _state.update { state ->
                                    state.copy(isPrivateChat = !isPublic)
                                }

                                ChatRoomChange.OwnPrivilege -> _state.update { state ->
                                    state.copy(myPermission = ownPrivilege, isActive = isActive)
                                }

                                ChatRoomChange.OpenInvite -> _state.update { state ->
                                    state.copy(isOpenInvite = isOpenInvite)
                                }

                                ChatRoomChange.Closed -> _state.update { state ->
                                    state.copy(myPermission = ownPrivilege, isActive = isActive)
                                }

                                ChatRoomChange.Archive -> _state.update { state ->
                                    state.copy(isArchived = isArchived)
                                }

                                else -> {}
                            }
                        }
                    }
                }
        }
    }

    private fun monitorNotificationMute(chatId: Long) {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect { changed ->
                if (changed) {
                    getNotificationMute(chatId)
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

    private fun checkIfIsParticipatingInACall() {
        viewModelScope.launch {
            runCatching { isParticipatingInChatCallUseCase() }
                .onSuccess {
                    _state.update { state -> state.copy(isParticipatingInACall = it) }
                }.onFailure { Timber.e(it) }
        }
    }

    private fun checkIfIsParticipatingInACallInThisChat(chatId: Long) {
        viewModelScope.launch {
            runCatching { hasACallInThisChatByChatIdUseCase(chatId) }
                .onSuccess {
                    _state.update { state -> state.copy(hasACallInThisChat = it) }
                }.onFailure { Timber.e(it) }
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

    /**
     * Handle action press
     *
     * @param action [ChatRoomMenuAction].
     */
    fun handleActionPress(action: ChatRoomMenuAction) {
        when (action) {
            is ChatRoomMenuAction.AudioCall -> {}
            is ChatRoomMenuAction.VideoCall -> {}
            is ChatRoomMenuAction.AddParticipants -> {}
        }
    }
}
