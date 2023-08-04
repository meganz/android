package mega.privacy.android.app.presentation.meeting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.getDayAndMonth
import mega.privacy.android.app.presentation.extensions.getEndZoneDateTime
import mega.privacy.android.app.presentation.extensions.getStartZoneDateTime
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastScheduledMeetingCanceledUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingOccurrenceUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.IsChatHistoryEmptyUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomUseCase
import mega.privacy.android.domain.usecase.meeting.UpdateOccurrenceUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import java.time.ZonedDateTime
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
 * @property queryChatLink                              [QueryChatLink]
 * @property removeChatLink                             [RemoveChatLink]
 * @property createChatLink                             [CreateChatLink]
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property monitorChatListItemUpdates                 [MonitorChatListItemUpdates]
 * @property megaChatApiGateway                         [MegaChatApiGateway]
 * @property updateOccurrenceUseCase                    [UpdateOccurrenceUseCase]
 * @property setWaitingRoomUseCase                      [SetWaitingRoomUseCase]
 * @property monitorChatRoomUpdates                     [MonitorChatRoomUpdates]
 * @property broadcastScheduledMeetingCanceledUseCase   [BroadcastScheduledMeetingCanceledUseCase]
 * @property getFeatureFlagValue                        [GetFeatureFlagValueUseCase]
 * @property state                                      Current view state as [ScheduledMeetingManagementState]setWaitingRoom
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
    private val queryChatLink: QueryChatLink,
    private val removeChatLink: RemoveChatLink,
    private val createChatLink: CreateChatLink,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorChatListItemUpdates: MonitorChatListItemUpdates,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val updateOccurrenceUseCase: UpdateOccurrenceUseCase,
    private val setWaitingRoomUseCase: SetWaitingRoomUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val broadcastScheduledMeetingCanceledUseCase: BroadcastScheduledMeetingCanceledUseCase,
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ScheduledMeetingManagementState())
    val state: StateFlow<ScheduledMeetingManagementState> = _state

    private var isChatHistoryEmptyJob: Job? = null

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    init {
        viewModelScope.launch {
            _state.update { state ->
                getFeatureFlagValue(AppFeatures.WaitingRoomSettings).let { flag ->
                    state.copy(
                        isWaitingRoomFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

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
    fun onOccurrenceTap(occurrence: ChatScheduledMeetingOccurr) =
        _state.update { state ->
            state.copy(
                selectedOccurrence = occurrence,
                editedOccurrence = occurrence,
                selectOccurrenceEvent = triggered
            )
        }

    /**
     * Cancel occurrence option selected
     */
    fun onCancelOccurrenceTap() =
        _state.update { state -> state.copy(cancelOccurrenceTapped = true) }

    /**
     * Edit occurrence option selected
     */
    fun onEditOccurrenceTap() =
        _state.update { state -> state.copy(editOccurrenceTapped = true) }

    /**
     * Reset selected occurrence
     */
    fun onResetSelectedOccurrence() = _state.update { state ->
        state.copy(
            selectedOccurrence = null,
            editedOccurrence = null,
            cancelOccurrenceTapped = false,
            editOccurrenceTapped = false
        )
    }

    /**
     * Consume select occurrence event
     */
    fun onConsumeSelectOccurrenceEvent() =
        _state.update { state -> state.copy(selectOccurrenceEvent = consumed) }

    /**
     * Cancel a scheduled meeting
     */
    fun onCancelScheduledMeeting() {
        _state.value.isChatHistoryEmpty?.let { isChatHistoryEmpty ->
            if (isChatHistoryEmpty) {
                cancelAndArchiveMeeting()
            } else {
                cancelMeeting()
            }
        }
    }

    /**
     * Cancel and archive a meeting
     */
    private fun cancelAndArchiveMeeting() = viewModelScope.launch {
        runCatching {
            state.value.chatId?.let { chatId ->
                cancelScheduledMeetingUseCase(chatId)
                archiveChatUseCase(chatId, true)
            }
        }.onSuccess {
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_cancel_scheduled_meeting_chat_history_empty_success_snackbar))
        }.onFailure { exception ->
            Timber.e(exception)
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
        }
    }

    /**
     * Cancel a meeting
     */
    private fun cancelMeeting() = viewModelScope.launch {
        runCatching {
            state.value.chatId?.let { chatId -> cancelScheduledMeetingUseCase(chatId) }
        }.onSuccess {
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_cancel_scheduled_meeting_chat_history_not_empty_success_snackbar))
        }.onFailure { exception ->
            Timber.e(exception)
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
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
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
        }
    }

    /**
     * Cancel last occurrence and the scheduled meeting
     */
    fun cancelOccurrenceAndScheduledMeeting() {
        _state.value.isChatHistoryEmpty?.let { isChatHistoryEmpty ->
            if (isChatHistoryEmpty) {
                cancelOccurrenceAndArchiveMeeting()
            } else {
                cancelOccurrenceAndMeeting()
            }
        }
    }

    /**
     * Cancel and archive meeting (in case of a meeting with a single occurrence)
     */
    private fun cancelOccurrenceAndArchiveMeeting() = viewModelScope.launch {
        runCatching {
            _state.value.chatId?.let { chatId ->
                cancelScheduledMeetingUseCase(chatId)
                archiveChatUseCase(chatId, true)
            }
        }.onSuccess {
            _state.update { it.copy(finish = true) }
            broadcastScheduledMeetingCanceledUseCase(R.string.meetings_cancel_scheduled_meeting_chat_history_empty_success_snackbar)
        }.onFailure { exception ->
            Timber.e(exception)
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
        }
    }

    /**
     * Cancel meeting (in case of a meeting with a single occurrence)
     */
    private fun cancelOccurrenceAndMeeting() = viewModelScope.launch {
        runCatching {
            _state.value.chatId?.let { chatId ->
                cancelScheduledMeetingUseCase(chatId)
            }
        }.onSuccess {
            _state.update { it.copy(finish = true) }
            broadcastScheduledMeetingCanceledUseCase(R.string.meetings_cancel_scheduled_meeting_chat_history_not_empty_success_snackbar)
        }.onFailure { exception ->
            Timber.e(exception)
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
        }
    }

    /**
     * Enable or disable waiting room option
     */
    fun setWaitingRoom(isWaitingRoom: Boolean = !state.value.enabledWaitingRoomOption) =
        state.value.chatId?.let { chatId ->
            viewModelScope.launch {
                runCatching {
                    setWaitingRoomUseCase(chatId, isWaitingRoom)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
                }.onSuccess {
                    _state.update { state ->
                        state.copy(
                            enabledWaitingRoomOption = isWaitingRoom
                        )
                    }
                }
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
        if (newChatId != megaChatApiGateway.getChatInvalidHandle() && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getChatRoom()
        }
    }

    /**
     * Sets the selected [ChatRoomItem]
     *
     * @param chatRoomItem The selected [ChatRoomItem]
     */
    fun setChatRoomItem(chatRoomItem: ChatRoomItem) =
        _state.update { state -> state.copy(chatRoomItem = chatRoomItem) }

    /**
     * Sets chatRoomItem as consumed.
     */
    fun setOnChatRoomItemConsumed() =
        _state.update { state -> state.copy(chatRoomItem = null) }

    /**
     * Get chat room of the scheduled meeting
     */
    private fun getChatRoom() = viewModelScope.launch {
        runCatching {
            _state.value.chatId?.let { chatId -> getChatRoomUseCase(chatId) }
        }.onSuccess { chatRoom ->
            chatRoom?.let { chat ->
                _state.update {
                    it.copy(
                        chatRoom = chat,
                        enabledWaitingRoomOption = chat.isWaitingRoom,
                    )
                }

                getChatRoomUpdates(chat.chatId)
                queryChatLink()
                getChatListItemUpdates()
            }

        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Get chat room updates
     *
     * @param chatId Chat id.
     */
    private fun getChatRoomUpdates(chatId: Long) =
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId).collectLatest { chat ->
                if (chat.hasChanged(ChatRoomChange.WaitingRoom)) {
                    _state.update {
                        it.copy(
                            enabledWaitingRoomOption = chat.isWaitingRoom,
                        )
                    }
                }
            }
        }

    /**
     * Get chat list item updates
     */
    private fun getChatListItemUpdates() =
        viewModelScope.launch {
            monitorChatListItemUpdates().collectLatest { item ->
                when (item.changes) {
                    ChatListItemChanges.LastMessage -> {
                        if (item.lastMessageType == ChatRoomLastMessage.PublicHandleCreate ||
                            item.lastMessageType == ChatRoomLastMessage.PublicHandleDelete
                        ) {
                            queryChatLink()
                        }
                    }

                    else -> {}
                }
            }
        }

    /**
     * Create or removed meeting link if there is internet connection, shows an error if not.
     */
    fun onMeetingLinkTap() {
        if (isConnected) {
            Timber.d("Meeting link option")
            if (_state.value.enabledMeetingLinkOption) {
                removeChatLink()
            } else {
                createChatLink()
            }
        } else {
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.check_internet_connection_error))
        }
    }

    /**
     * Check if there is an existing chat-link for an public chat
     */
    private fun queryChatLink() {
        _state.value.chatId?.let { id ->
            viewModelScope.launch {
                runCatching {
                    queryChatLink(id)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Query chat link successfully")
                    _state.update {
                        it.copy(
                            enabledMeetingLinkOption = request.text != null,
                            meetingLink = request.text
                        )
                    }
                }
            }
        }
    }

    /**
     * Remove chat link
     */
    private fun removeChatLink() {
        _state.value.chatId?.let { id ->
            viewModelScope.launch {
                runCatching {
                    removeChatLink(id)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
                }.onSuccess { _ ->
                    Timber.d("Remove chat link successfully")
                    _state.update { it.copy(enabledMeetingLinkOption = false, meetingLink = null) }
                }
            }
        }
    }

    /**
     * Scheduled meeting updated
     */
    fun scheduledMeetingUpdated() {
        triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_edit_scheduled_meeting_success_snackbar))
    }

    /**
     * Scheduled meeting occurrence updated
     */
    fun onUpdateScheduledMeetingOccurrenceTap() =
        state.value.editedOccurrence?.let { editedOccurrence ->
            state.value.selectedOccurrence?.let { selectedOccurrence ->
                selectedOccurrence.startDateTime?.let { overrides ->
                    state.value.chatId?.let { id ->
                        viewModelScope.launch {
                            runCatching {
                                updateOccurrenceUseCase(
                                    chatId = id,
                                    occurrence = editedOccurrence,
                                    overrides = overrides
                                )
                            }.onSuccess {
                                triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_update_scheduled_meeting_occurrence_success_snackbar))
                            }.onFailure { exception ->
                                Timber.e(exception)
                                triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
                            }
                        }
                    }
                }
            }
        }

    /**
     * Set start time
     *
     * @param newStartTime          Start time
     */
    fun onNewStartTime(newStartTime: ZonedDateTime) =
        state.value.editedOccurrence?.getStartZoneDateTime()?.let { currentStartZonedDateTime ->
            val newStartZonedDateTime =
                currentStartZonedDateTime.withHour(newStartTime.hour)
                    .withMinute(newStartTime.minute)

            if (currentStartZonedDateTime.isEqual(newStartZonedDateTime)) {
                return@let
            }

            state.value.editedOccurrence?.getEndZoneDateTime()?.let { currentEndZonedDateTime ->
                var newEndZonedDateTime = currentEndZonedDateTime
                if (newStartZonedDateTime.isEqual(currentEndZonedDateTime) || newStartZonedDateTime.isAfter(
                        currentEndZonedDateTime
                    )
                ) {
                    newEndZonedDateTime = newStartZonedDateTime.plusMinutes(30)
                }

                _state.update { state ->
                    state.copy(
                        editedOccurrence = state.editedOccurrence?.copy(
                            startDateTime = newStartZonedDateTime.toEpochSecond(),
                            endDateTime = newEndZonedDateTime.toEpochSecond()
                        )
                    )
                }
            }
        }

    /**
     * Set end time
     *
     * @param newEndTime          End time
     */
    fun onNewEndTime(newEndTime: ZonedDateTime) =
        state.value.editedOccurrence?.getEndZoneDateTime()?.let { currentEndZonedDateTime ->
            val newEndZonedDateTime =
                currentEndZonedDateTime.withHour(newEndTime.hour)
                    .withMinute(newEndTime.minute)

            if (currentEndZonedDateTime.isEqual(newEndZonedDateTime)) {
                return@let
            }

            state.value.editedOccurrence?.getStartZoneDateTime()?.let { currentStartZonedDateTime ->
                var newStartZonedDateTime = currentStartZonedDateTime
                if (newEndZonedDateTime.isEqual(currentStartZonedDateTime) || newEndZonedDateTime.isBefore(
                        currentStartZonedDateTime
                    )
                ) {
                    newStartZonedDateTime = newEndZonedDateTime.minusMinutes(30)
                }

                _state.update { state ->
                    state.copy(
                        editedOccurrence = state.editedOccurrence?.copy(
                            startDateTime = newStartZonedDateTime.toEpochSecond(),
                            endDateTime = newEndZonedDateTime.toEpochSecond()
                        )
                    )
                }
            }
        }

    /**
     * Set start date
     *
     * @param newStartDate          Start date
     */
    fun onNewStartDate(newStartDate: ZonedDateTime) =
        state.value.editedOccurrence?.getStartZoneDateTime()?.let { currentStartZonedDateTime ->
            val newStartZonedDateTime =
                newStartDate.withHour(currentStartZonedDateTime.hour)
                    .withMinute(currentStartZonedDateTime.minute)

            if (currentStartZonedDateTime.isEqual(newStartZonedDateTime)) {
                return@let
            }

            _state.update { state ->
                state.copy(
                    editedOccurrence = state.editedOccurrence?.copy(
                        startDateTime = newStartZonedDateTime.toEpochSecond(),
                    )
                )
            }
        }

    /**
     * Close waiting room warning
     */
    fun closeWaitingRoomWarning() =
        _state.update {
            it.copy(isWarningClosed = true)
        }

    /**
     * Shares the link to chat
     *
     * @param data       Intent containing the info to share the content to chats.
     * @param action     Action to perform.
     */
    fun sendToChat(
        data: Intent?,
        action: (Intent?) -> Unit,
    ) {
        data?.putExtra(Constants.EXTRA_LINK, _state.value.meetingLink)
        action.invoke(data)
    }

    /**
     * Copy meeting link to clipboard
     *
     * @param clipboard [ClipboardManager]
     */
    fun copyMeetingLink(clipboard: ClipboardManager) {
        _state.value.meetingLink?.let { meetingLink ->
            val clip = ClipData.newPlainText(Constants.COPIED_TEXT_LABEL, meetingLink)
            clipboard.setPrimaryClip(clip)
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.scheduled_meetings_meeting_link_copied))
        }
    }

    /**
     * Create chat link
     */
    private fun createChatLink() {
        _state.value.chatId?.let { id ->
            viewModelScope.launch {
                runCatching {
                    createChatLink(id)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_text_error))
                }.onSuccess { request ->
                    _state.update {
                        it.copy(
                            enabledMeetingLinkOption = true,
                            meetingLink = request.text
                        )
                    }
                }
            }
        }
    }
}