package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getDayAndMonth
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingOccurrenceUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.IsChatHistoryEmptyUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Scheduled meeting management view model
 *
 * @property isChatHistoryEmptyUseCase                  [IsChatHistoryEmptyUseCase]
 * @property loadMessagesUseCase                        [LoadMessagesUseCase]
 * @property archiveChatUseCase                         [ArchiveChatUseCase]
 * @property cancelScheduledMeetingUseCase              [CancelScheduledMeetingUseCase]
 * @property cancelScheduledMeetingOccurrenceUseCase    [CancelScheduledMeetingOccurrenceUseCase]
 * @property getChatRoomUseCase                         [GetChatRoom]
 * @property getStringFromStringResMapper               [GetStringFromStringResMapper]
 * @property state                                      Current view state as [ScheduledMeetingManagementState]
 */
@HiltViewModel
class ScheduledMeetingManagementViewModel @Inject constructor(
    private val isChatHistoryEmptyUseCase: IsChatHistoryEmptyUseCase,
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val cancelScheduledMeetingUseCase: CancelScheduledMeetingUseCase,
    private val cancelScheduledMeetingOccurrenceUseCase: CancelScheduledMeetingOccurrenceUseCase,
    private val getChatRoomUseCase: GetChatRoom,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(ScheduledMeetingManagementState())
    val state: StateFlow<ScheduledMeetingManagementState> = _state

    private var isChatHistoryEmptyJob: Job? = null

    /**
     * Checks if chat history is empty (only management messages)
     *
     * @param chatId    Chat ID
     * @return          True if the chat history is empty (only management messages) or false otherwise.
     */
    fun checkIfIsChatHistoryEmpty(chatId: Long) = viewModelScope.launch {
        loadMessagesUseCase(chatId)
    }

    /**
     * Starts monitoring load messages in case cancel meeting option is taken.
     * For that [IsChatHistoryEmptyUseCase] is called.
     *
     * @param chatId Chat ID.
     */
    fun monitorLoadedMessages(chatId: Long) {
        isChatHistoryEmptyJob = viewModelScope.launch {
            isChatHistoryEmptyUseCase(chatId).catch { Timber.e(it) }.collect { isChatHistoryEmpty ->
                Timber.d("checkIfIsChatHistoryEmpty: $isChatHistoryEmpty")
                _state.update { state ->
                    state.copy(
                        isChatHistoryEmpty = isChatHistoryEmpty,
                        chatId = chatId
                    )
                }
            }
        }
    }

    /**
     * Sets isChatHistoryEmpty as consumed.
     */
    fun setOnChatHistoryEmptyConsumed() {
        stopMonitoringLoadMessages()
        _state.update { state -> state.copy(isChatHistoryEmpty = null) }
    }

    /**
     * Sets chatId as consumed.
     */
    fun setOnChatIdConsumed() {
        _state.update { state -> state.copy(chatId = null) }
    }

    /**
     * Stops monitoring load messages.
     */
    fun stopMonitoringLoadMessages() {
        isChatHistoryEmptyJob?.cancel()
    }

    /**
     * Display occurrence's options
     */
    fun onOccurrenceTap(occurrence: ChatScheduledMeetingOccurr) = _state.update { state ->
        state.copy(
            selectedOccurrence = occurrence, selectOccurrenceEvent = triggered
        )
    }

    /**
     * Cancel occurrence option selected
     */
    fun onCancelOccurrenceTap() = _state.update { state -> state.copy(displayDialog = true) }

    /**
     * Reset selected occurrence
     */
    fun onResetSelectedOccurrence() =
        _state.update { state -> state.copy(selectedOccurrence = null, displayDialog = false) }

    /**
     * Consume select occurrence event
     */
    fun onConsumeSelectOccurrenceEvent() =
        _state.update { state -> state.copy(selectOccurrenceEvent = consumed) }

    /**
     * Cancel and archive a meeting
     */
    fun cancelAndArchiveMeeting() = viewModelScope.launch {
        runCatching {
            state.value.chatId?.let { chatId ->
                cancelScheduledMeetingUseCase(chatId)
                archiveChatUseCase(chatId, true)
            }
        }.onSuccess {
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_cancel_scheduled_meeting_chat_history_empty_success_snackbar))
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Cancel a meeting
     */
    fun cancelMeeting() = viewModelScope.launch {
        runCatching {
            state.value.chatId?.let { chatId -> cancelScheduledMeetingUseCase(chatId) }
        }.onSuccess {
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_cancel_scheduled_meeting_chat_history_not_empty_success_snackbar))
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Cancel an occurrence (in case of a meeting with multiple occurrences)
     *
     * @param occurrence    Occurrence to cancel
     */
    fun cancelOccurrence(occurrence: ChatScheduledMeetingOccurr) = viewModelScope.launch {
        runCatching {
            _state.value.chatId?.let { chatId ->
                cancelScheduledMeetingOccurrenceUseCase(
                    chatId,
                    occurrence
                )
            }
        }.onSuccess {
            triggerSnackbarMessage(
                getStringFromStringResMapper(
                    R.string.meetings_cancel_scheduled_meeting_occurrence_success_snackbar,
                    occurrence.getDayAndMonth().orEmpty()
                )
            )
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Cancel occurrence and archive meeting (in case of a meeting with a single occurrence)
     *
     * @param occurrence    Occurrence to cancel
     */
    fun cancelOccurrenceAndArchiveMeeting(occurrence: ChatScheduledMeetingOccurr) =
        viewModelScope.launch {
            runCatching {
                _state.value.chatId?.let { chatId ->
                    cancelScheduledMeetingOccurrenceUseCase(chatId, occurrence)
                    cancelScheduledMeetingUseCase(chatId)
                    archiveChatUseCase(chatId, true)
                }
            }.onSuccess {
                _state.update { it.copy(finish = true) }
                triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_cancel_scheduled_meeting_chat_history_empty_success_snackbar))
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Trigger event to show Snackbar message
     *
     * @param message     Content for snack bar
     */
    private fun triggerSnackbarMessage(message: String) =
        _state.update { it.copy(snackbarMessageContent = triggered(message)) }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onSnackbarMessageConsumed() =
        _state.update {
            it.copy(snackbarMessageContent = consumed())
        }

    /**
     * Sets chat ID
     *
     * @param newChatId Chat ID.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getChatRoom()
        }
    }

    /**
     * Get chat room of the scheduled meeting
     */
    private fun getChatRoom() = viewModelScope.launch {
        runCatching {
            _state.value.chatId?.let { chatId -> getChatRoomUseCase(chatId) }
        }.onSuccess { chatRoom ->
            _state.update {
                it.copy(chatRoom = chatRoom)
            }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }
}