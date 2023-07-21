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
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.FetchNumberOfScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.FetchScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingOccurrencesUpdates
import timber.log.Timber
import javax.inject.Inject

/**
 * RecurringMeetingInfoActivity view model.
 * @property monitorConnectivityUseCase                             [MonitorConnectivityUseCase]
 * @property getScheduledMeetingByChat                              [GetScheduledMeetingByChat]
 * @property fetchScheduledMeetingOccurrencesByChat                 [FetchScheduledMeetingOccurrencesByChat]
 * @property fetchNumberOfScheduledMeetingOccurrencesByChat         [FetchNumberOfScheduledMeetingOccurrencesByChat]
 * @property getChatParticipants                                    [GetChatParticipants]
 * @property megaChatApiGateway                                     [MegaChatApiGateway]
 * @property monitorScheduledMeetingUpdates                         [MonitorScheduledMeetingUpdates]
 * @property monitorScheduledMeetingOccurrencesUpdates              [MonitorScheduledMeetingOccurrencesUpdates]
 * @property state                                                  Current view state as [RecurringMeetingInfoState]
 */
@HiltViewModel
class RecurringMeetingInfoViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val fetchScheduledMeetingOccurrencesByChat: FetchScheduledMeetingOccurrencesByChat,
    private val fetchNumberOfScheduledMeetingOccurrencesByChat: FetchNumberOfScheduledMeetingOccurrencesByChat,
    private val getChatParticipants: GetChatParticipants,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val deviceGateway: DeviceGateway,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val monitorScheduledMeetingOccurrencesUpdates: MonitorScheduledMeetingOccurrencesUpdates,
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringMeetingInfoState())
    val state: StateFlow<RecurringMeetingInfoState> = _state

    private val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

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
            getScheduledMeeting()
            loadAllChatParticipants()
        }
    }

    /**
     * Get scheduled meeting
     */
    private fun getScheduledMeeting() =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Scheduled meeting does not exist, finish $exception")
                finishActivity()
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (isMainScheduledMeeting(scheduledMeetReceived)) {
                            Timber.d("Scheduled meeting exists")
                            updateScheduledMeeting(scheduledMeetReceived)
                            getOccurrencesUpdates()
                            getScheduledMeetingUpdates()
                            getOccurrences(needsToRefreshOccurs = false)
                            return@forEach
                        }
                    }
                }
            }
        }

    /**
     * Load all chat participants
     */
    private fun loadAllChatParticipants() = viewModelScope.launch {
        runCatching {
            getChatParticipants(state.value.chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    Timber.d("Updated first and second participant")
                    _state.update {
                        it.copy(
                            firstParticipant = list.firstOrNull(),
                            secondParticipant = list.getOrNull(1)
                        )
                    }
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Load all occurrences
     *
     * @param needsToRefreshOccurs     True, if needs to refresh the occurrences. False, if not.
     */
    private fun getOccurrences(needsToRefreshOccurs: Boolean = false) =
        viewModelScope.launch {
            runCatching {
                when {
                    needsToRefreshOccurs -> {
                        fetchNumberOfScheduledMeetingOccurrencesByChat(
                            state.value.chatId,
                            state.value.occurrencesList.size
                        )
                    }

                    state.value.isEmptyOccurrencesList() -> {
                        fetchScheduledMeetingOccurrencesByChat(
                            state.value.chatId,
                            0
                        )
                    }

                    else -> state.value.occurrencesList.last().startDateTime?.let {
                        fetchScheduledMeetingOccurrencesByChat(state.value.chatId, it)
                    }
                }
            }.onFailure { exception ->
                Timber.e("Error retrieving list of occurrences: $exception")
            }.onSuccess { list ->
                list?.let { listOccurrences ->
                    Timber.d("List of occurrences successfully retrieved. Number new occurrences: ${listOccurrences.size - 1}")
                    val newList =
                        if (needsToRefreshOccurs) mutableListOf() else state.value.occurrencesList.toMutableList()

                    for (occurrence in listOccurrences) {
                        if (!occurrence.isCancelled && !newList.contains(
                                occurrence
                            )
                        ) {
                            newList.add(occurrence)
                        }
                    }

                    _state.update {
                        it.copy(occurrencesList = newList, is24HourFormat = is24HourFormat)
                    }

                    checkSeeMoreVisibility(listOccurrences.size)
                }
            }
        }

    /**
     * Update scheduled meeting
     *
     * @param scheduledMeetReceived [ChatScheduledMeeting]
     */
    private fun updateScheduledMeeting(scheduledMeetReceived: ChatScheduledMeeting) {
        var freq = OccurrenceFrequencyType.Invalid
        var until = 0L
        scheduledMeetReceived.rules?.let { rules ->
            freq = rules.freq
            until = rules.until
        }

        _state.update {
            it.copy(
                schedTitle = scheduledMeetReceived.title,
                schedId = scheduledMeetReceived.schedId,
                schedUntil = until,
                typeOccurs = freq
            )
        }
    }

    /**
     * Check if is the current scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's same. False if not.
     */
    private fun isSameScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        state.value.chatId == scheduledMeet.chatId

    /**
     * Check if is main scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's the main scheduled meeting. False if not.
     */
    private fun isMainScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        scheduledMeet.parentSchedId == megaChatApiGateway.getChatInvalidHandle()

    /**
     * Get scheduled meeting updates
     */
    private fun getScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->
                if (!isSameScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                    return@collectLatest
                }

                if (!isMainScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                    return@collectLatest
                }

                Timber.d("Monitor scheduled meeting updated, changes ${scheduledMeetReceived.changes}")
                when (val changes = scheduledMeetReceived.changes) {
                    ScheduledMeetingChanges.NewScheduledMeeting -> updateScheduledMeeting(
                        scheduledMeetReceived = scheduledMeetReceived
                    )

                    ScheduledMeetingChanges.Title,
                    ScheduledMeetingChanges.RepetitionRules,
                    ->
                        if (scheduledMeetReceived.schedId == state.value.schedId) {
                            when (changes) {
                                ScheduledMeetingChanges.Title -> {
                                    _state.update {
                                        it.copy(
                                            schedTitle = scheduledMeetReceived.title,
                                        )
                                    }
                                }

                                ScheduledMeetingChanges.RepetitionRules -> {
                                    var freq = OccurrenceFrequencyType.Invalid
                                    var until = 0L

                                    scheduledMeetReceived.rules?.let { rules ->
                                        freq = rules.freq
                                        until = rules.until
                                    }

                                    _state.update {
                                        it.copy(
                                            schedUntil = until,
                                            typeOccurs = freq,
                                        )
                                    }
                                }

                                else -> {}
                            }

                        }

                    else -> {}
                }
            }
        }

    /**
     * Get occurrences updates
     */
    private fun getOccurrencesUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingOccurrencesUpdates().collectLatest { result ->
                if (!result.append && state.value.chatId == result.chatId) {
                    getOccurrences(needsToRefreshOccurs = true)
                }
            }
        }

    /**
     * Control the visibility of the button see more occurrences
     */
    private fun checkSeeMoreVisibility(sizeNewOccurrences: Int) {
        if (sizeNewOccurrences < 20) {
            _state.update {
                it.copy(showSeeMoreButton = false)
            }
            return
        }

        state.value.schedUntil.let { until ->
            if (until != 0L) {
                state.value.occurrencesList.last().startDateTime?.let { time ->
                    _state.update {
                        it.copy(showSeeMoreButton = time < until)
                    }
                    return
                }
            }
        }

        _state.update {
            it.copy(showSeeMoreButton = true)
        }
    }

    /**
     * See more occurrences
     */
    fun onSeeMoreOccurrencesTap() {
        Timber.d("Get more occurrences")
        getOccurrences(needsToRefreshOccurs = false)
    }

    /**
     * Finish activity
     */
    private fun finishActivity() =
        _state.update { state ->
            state.copy(finish = true)
        }
}