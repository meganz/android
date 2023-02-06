package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.MonitorConnectivity
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * RecurringMeetingInfoActivity view model.
 * @property monitorConnectivity            [MonitorConnectivity]
 * @property getScheduledMeetingByChat      [GetScheduledMeetingByChat]
 * @property getChatParticipants            [GetChatParticipants]
 * @property state    Current view state as [RecurringMeetingInfoState]
 */
@HiltViewModel
class RecurringMeetingInfoViewModel @Inject constructor(
    private val monitorConnectivity: MonitorConnectivity,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatParticipants: GetChatParticipants,
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringMeetingInfoState())
    val state: StateFlow<RecurringMeetingInfoState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Sets chat id
     *
     * @param newChatId                 Chat id.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getScheduledMeeting(newChatId)
            loadAllChatParticipants(newChatId)
        }
    }

    /**
     * Get scheduled meeting
     *
     * @param chatId Chat id.
     */
    private fun getScheduledMeeting(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(chatId)
            }.onFailure { exception ->
                Timber.e("Scheduled meeting does not exist, finish $exception")
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                            Timber.d("Scheduled meeting exists")
                            _state.update {
                                it.copy(
                                    schedTitle = scheduledMeetReceived.title,
                                    schedId = scheduledMeetReceived.schedId
                                )
                            }
                            return@forEach
                        }
                    }
                }
            }
        }

    /**
     * Load all chat participants
     */
    private fun loadAllChatParticipants(chatId: Long) = viewModelScope.launch {
        runCatching {
            getChatParticipants(chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    Timber.d("Updated first and second participant")
                    _state.update {
                        it.copy(
                            firstParticipant = if (list.isNotEmpty()) list.first() else null,
                            secondParticipant = if (list.size > 1) list[1] else null
                        )
                    }
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }
}