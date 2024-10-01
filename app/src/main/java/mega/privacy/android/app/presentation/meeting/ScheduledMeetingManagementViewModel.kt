package mega.privacy.android.app.presentation.meeting

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.getLink.BaseLinkViewModel
import mega.privacy.android.app.presentation.extensions.getDayAndMonth
import mega.privacy.android.app.presentation.extensions.getEndZoneDateTime
import mega.privacy.android.app.presentation.extensions.getStartZoneDateTime
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.model.ShareLinkOption
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.IsDevice24HourFormatUseCase
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.QueryChatLinkUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.CreateChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.link.RemoveChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastScheduledMeetingCanceledUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingOccurrenceUseCase
import mega.privacy.android.domain.usecase.meeting.CancelScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.GetWaitingRoomRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.IsChatHistoryEmptyUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.UpdateOccurrenceUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
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
 * @property getChatRoomUseCase                         [GetChatRoomUseCase]
 * @property getStringFromStringResMapper               [GetStringFromStringResMapper]
 * @property queryChatLinkUseCase                       [QueryChatLinkUseCase]
 * @property removeChatLinkUseCase                      [RemoveChatLinkUseCase]
 * @property createChatLinkUseCase                      [CreateChatLinkUseCase]
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property monitorChatListItemUpdates                 [MonitorChatListItemUpdates]
 * @property megaChatApiGateway                         [MegaChatApiGateway]
 * @property updateOccurrenceUseCase                    [UpdateOccurrenceUseCase]
 * @property broadcastScheduledMeetingCanceledUseCase   [BroadcastScheduledMeetingCanceledUseCase]
 * @property getFeatureFlagValue                        [GetFeatureFlagValueUseCase]
 * @property getWaitingRoomRemindersUseCase             [GetWaitingRoomRemindersUseCase]
 * @property setWaitingRoomRemindersUseCase             [SetWaitingRoomRemindersUseCase]
 * @property monitorChatCallUpdatesUseCase              [MonitorChatCallUpdatesUseCase]
 * @property getChatCallUseCase                         [GetChatCallUseCase]
 * @property getCurrentSubscriptionPlanUseCase          [GetCurrentSubscriptionPlanUseCase]
 * @property monitorAccountDetailUseCase                [MonitorAccountDetailUseCase]
 * @property state                                      Current view state as [ScheduledMeetingManagementUiState]setWaitingRoom
 */
@HiltViewModel
class ScheduledMeetingManagementViewModel @Inject constructor(
    private val isChatHistoryEmptyUseCase: IsChatHistoryEmptyUseCase,
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val cancelScheduledMeetingUseCase: CancelScheduledMeetingUseCase,
    private val cancelScheduledMeetingOccurrenceUseCase: CancelScheduledMeetingOccurrenceUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val queryChatLinkUseCase: QueryChatLinkUseCase,
    private val removeChatLinkUseCase: RemoveChatLinkUseCase,
    private val createChatLinkUseCase: CreateChatLinkUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorChatListItemUpdates: MonitorChatListItemUpdates,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val updateOccurrenceUseCase: UpdateOccurrenceUseCase,
    private val broadcastScheduledMeetingCanceledUseCase: BroadcastScheduledMeetingCanceledUseCase,
    private val getWaitingRoomRemindersUseCase: GetWaitingRoomRemindersUseCase,
    private val setWaitingRoomRemindersUseCase: SetWaitingRoomRemindersUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
    private val isDevice24HourFormatUseCase: IsDevice24HourFormatUseCase,
    get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    sendTextMessageUseCase: SendTextMessageUseCase,
) : BaseLinkViewModel(get1On1ChatIdUseCase, sendTextMessageUseCase) {
    private val _state = MutableStateFlow(ScheduledMeetingManagementUiState())
    val state: StateFlow<ScheduledMeetingManagementUiState> = _state

    private var isChatHistoryEmptyJob: Job? = null

    internal var chatScheduledMeeting: ChatScheduledMeeting? = null

    internal val is24HourFormat: Boolean
        get() = isDevice24HourFormatUseCase()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        getMyFullName()
        checkWaitingRoomWarning()
        getApiFeatureFlag()

        viewModelScope.launch {
            getCurrentSubscriptionPlanUseCase()?.let { currentSubscriptionPlan ->
                _state.update { it.copy(subscriptionPlan = currentSubscriptionPlan) }
            }
        }
        getAccountDetailUpdates()

        viewModelScope.launch {
            flow {
                emitAll(monitorUserUpdates()
                    .catch { Timber.w("Exception monitoring user updates: $it") }
                    .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email })
            }.collect {
                when (it) {
                    UserChanges.Firstname, UserChanges.Lastname -> getMyFullName()
                    else -> Unit
                }
            }
        }
    }

    /**
     * Get my full name
     */
    private fun getMyFullName() = viewModelScope.launch {
        runCatching {
            getMyFullNameUseCase()
        }.onSuccess {
            it?.apply {
                _state.update { state ->
                    state.copy(
                        myFullName = this,
                    )
                }
            }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Get call unlimited pro plan api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValue(ApiFeatures.CallUnlimitedProPlan)
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

    /**
     * Get account detail updates
     */
    private fun getAccountDetailUpdates() = viewModelScope.launch {
        monitorAccountDetailUseCase().catch { Timber.e(it) }
            .collectLatest { accountDetail ->
                accountDetail.levelDetail?.accountType?.let { subscriptionPlan ->
                    _state.update { it.copy(subscriptionPlan = subscriptionPlan) }
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
        monitorLoadedMessages(chatId)
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
        if (newChatId != megaChatApiGateway.getChatInvalidHandle()) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getChatRoom()
            getChatCall()
        }
    }

    private fun getChatCall() = viewModelScope.launch {
        runCatching {
            _state.value.chatId?.let { chatId -> getChatCallUseCase(chatId) }
        }.onSuccess { chatCall ->
            chatCall?.let { call ->
                call.status?.let {
                    checkCallStatus(it)
                }
                getChatCallUpdates()
            }

        }.onFailure { exception ->
            Timber.e(exception)
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
                    )
                }

                queryChatLink()
                getChatListItemUpdates()
            }

        }.onFailure { exception ->
            Timber.e(exception)
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
                    queryChatLinkUseCase(id)
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
                    removeChatLinkUseCase(id)
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
            val nowZonedDateTime: ZonedDateTime = ZonedDateTime.now()
            if (newStartTime.isBefore(nowZonedDateTime) || currentStartZonedDateTime.isEqual(
                    newStartTime
                )
            ) {
                return@let
            }

            state.value.editedOccurrence?.getEndZoneDateTime()?.let { currentEndZonedDateTime ->
                var newEndZonedDateTime = currentEndZonedDateTime
                if (newStartTime.isEqual(currentEndZonedDateTime) || newStartTime.isAfter(
                        currentEndZonedDateTime
                    )
                ) {
                    newEndZonedDateTime = newStartTime.plusMinutes(30)
                }

                _state.update { state ->
                    state.copy(
                        editedOccurrence = state.editedOccurrence?.copy(
                            startDateTime = newStartTime.toEpochSecond(),
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
            if (currentEndZonedDateTime.isEqual(newEndTime)) {
                return@let
            }

            state.value.editedOccurrence?.getStartZoneDateTime()?.let { currentStartZonedDateTime ->
                var newStartZonedDateTime = currentStartZonedDateTime
                if (newEndTime.isEqual(currentStartZonedDateTime) || newEndTime.isBefore(
                        currentStartZonedDateTime
                    )
                ) {
                    newStartZonedDateTime = newEndTime.minusMinutes(30)
                }

                val nowZonedDateTime: ZonedDateTime = ZonedDateTime.now()
                if (!newStartZonedDateTime.isBefore(nowZonedDateTime) && !newEndTime.isBefore(
                        nowZonedDateTime
                    )
                ) {
                    _state.update { state ->
                        state.copy(
                            editedOccurrence = state.editedOccurrence?.copy(
                                startDateTime = newStartZonedDateTime.toEpochSecond(),
                                endDateTime = newEndTime.toEpochSecond()
                            )
                        )
                    }
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
            val nowZonedDateTime: ZonedDateTime = ZonedDateTime.now()
            if (newStartDate.isBefore(nowZonedDateTime) || currentStartZonedDateTime.isEqual(
                    newStartDate
                )
            ) {
                return@let
            }

            _state.update { state ->
                state.copy(
                    editedOccurrence = state.editedOccurrence?.copy(
                        startDateTime = newStartDate.toEpochSecond(),
                    )
                )
            }
        }

    /**
     * Close waiting room warning
     */
    fun closeWaitingRoomWarning() =
        viewModelScope.launch {
            runCatching {
                setWaitingRoomRemindersUseCase(WaitingRoomReminders.Disabled)
            }
            _state.update {
                it.copy(waitingRoomReminder = WaitingRoomReminders.Disabled)
            }
        }

    /**
     * Check waiting room warning
     */
    fun checkWaitingRoomWarning() {
        viewModelScope.launch {
            getWaitingRoomRemindersUseCase().collectLatest { result ->
                _state.update { it.copy(waitingRoomReminder = result) }
            }
        }
    }

    /**
     *  Check call status
     *
     * @param status    [ChatCallStatus]
     */
    private fun checkCallStatus(status: ChatCallStatus) {
        val isInProgress = when (status) {
            ChatCallStatus.Connecting,
            ChatCallStatus.Joining,
            ChatCallStatus.InProgress,
            -> true

            else -> false
        }
        _state.update {
            it.copy(
                isCallInProgress = isInProgress
            )
        }
    }

    /**
     * Get chat call updates
     */
    private fun getChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    call.changes?.apply {
                        Timber.d("Monitor chat call updated, changes ${call.changes}")
                        if (contains(ChatCallChanges.Status)) {
                            call.status?.let {
                                checkCallStatus(it)
                            }
                        }
                    }
                }
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
                    createChatLinkUseCase(id)
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

    /**
     * Trigger event to show meeting link share bottom sheet
     */
    private fun onMeetingLinkCreated() {
        _state.update { it.copy(meetingLinkCreated = triggered) }
    }

    /**
     * Reset and notify that meeting link share is consumed
     */
    fun onMeetingLinkShareShown() {
        _state.update { it.copy(meetingLinkCreated = consumed) }
    }

    /**
     * Set meeting link and start/end time
     */
    fun setMeetingLink(
        chatId: Long,
        link: String?,
        title: String,
    ) {
        _state.update { it.copy(meetingLink = link, chatId = chatId, title = title) }
        getScheduledMeeting()
    }

    /**
     * Share meeting link
     */
    fun onMeetingLinkShare(option: ShareLinkOption) {
        _state.update { it.copy(meetingLinkAction = triggered(option)) }
    }

    /**
     * Reset and notify that meeting link share is consumed
     */
    fun onMeetingLinkShareConsumed() {
        _state.update { it.copy(meetingLinkAction = consumed()) }
    }

    /**
     * Get scheduled meeting
     */
    private fun getScheduledMeeting() {
        _state.value.chatId?.let {
            viewModelScope.launch {
                runCatching {
                    getScheduledMeetingByChatUseCase(it)
                }.onFailure { exception ->
                    Timber.e("Scheduled meeting does not exist, finish $exception")
                }.onSuccess { scheduledMeetingList ->
                    scheduledMeetingList?.let { list ->
                        list.forEach { scheduledMeetReceived ->
                            if (isMainScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                                updateScheduledMeeting(scheduledMeetReceived = scheduledMeetReceived)
                                return@forEach
                            }
                        }
                    }
                }
                onMeetingLinkCreated()
            }
        }
    }

    /**
     * Check if is main scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's the main scheduled meeting. False if not.
     */
    private fun isMainScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        scheduledMeet.parentSchedId == megaChatApiGateway.getChatInvalidHandle()

    /**
     * Update scheduled meeting
     *
     * @param scheduledMeetReceived [ChatScheduledMeeting]
     */
    private fun updateScheduledMeeting(scheduledMeetReceived: ChatScheduledMeeting) {
        chatScheduledMeeting = scheduledMeetReceived
    }

}
