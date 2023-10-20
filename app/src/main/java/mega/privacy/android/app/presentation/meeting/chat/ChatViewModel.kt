package mega.privacy.android.app.presentation.meeting.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.chat.GetUserChatStatusByChatUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
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
 * @property getUserChatStatusByChatUseCase
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
    private val getUserChatStatusByChatUseCase: GetUserChatStatusByChatUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val chatId = savedStateHandle.get<Long>(Constants.CHAT_ID)

    init {
        chatId?.let {
            getChatRoom(it)
            getNotificationMute(it)
            monitorChatRoom(it)
            monitorNotificationMute(it)
        }
    }

    private fun getChatRoom(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let {
                    _state.update { state ->
                        state.copy(
                            title = chatRoom.title,
                            isPrivateChat = !chatRoom.isGroup || !chatRoom.isPublic
                        )
                    }
                    getUserChatStatus(chatRoom)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getUserChatStatus(chatRoom: ChatRoom) {
        viewModelScope.launch {
            runCatching {
                getUserChatStatusByChatUseCase(chatRoom)
            }.onSuccess { userChatStatus ->
                userChatStatus?.let {
                    _state.update { state -> state.copy(userChatStatus = it) }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getNotificationMute(chatId: Long) {
        viewModelScope.launch {
            val isMute = isChatNotificationMuteUseCase(chatId)
            _state.update { it.copy(isChatNotificationMute = isMute) }
        }
    }

    private fun monitorChatRoom(chatId: Long) {
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId)
                .collect {
                    it.changes?.forEach { change ->
                        if (change == ChatRoomChange.Title) {
                            _state.update { state -> state.copy(title = it.title) }
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
}
