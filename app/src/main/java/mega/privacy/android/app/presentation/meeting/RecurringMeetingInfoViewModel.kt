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
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.meeting.FetchScheduledMeetingOccurrencesByChat
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * RecurringMeetingInfoActivity view model.
 * @property monitorConnectivity                            [MonitorConnectivity]
 * @property getScheduledMeetingByChat                      [GetScheduledMeetingByChat]
 * @property fetchScheduledMeetingOccurrencesByChat         [FetchScheduledMeetingOccurrencesByChat]
 * @property getChatParticipants                            [GetChatParticipants]
 * @property megaChatApiGateway                             [MegaChatApiGateway]
 * @property state                                          Current view state as [RecurringMeetingInfoState]
 */
@HiltViewModel
class RecurringMeetingInfoViewModel @Inject constructor(
    private val monitorConnectivity: MonitorConnectivity,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val fetchScheduledMeetingOccurrencesByChat: FetchScheduledMeetingOccurrencesByChat,
    private val getChatParticipants: GetChatParticipants,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val deviceGateway: DeviceGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringMeetingInfoState())
    val state: StateFlow<RecurringMeetingInfoState> = _state

    private val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

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
                        if (scheduledMeetReceived.parentSchedId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                            Timber.d("Scheduled meeting exists")
                            var freq = OccurrenceFrequencyType.Invalid
                            var until: Long = -1
                            scheduledMeetReceived.rules?.let { rules ->
                                freq = rules.freq
                                until = rules.until ?: -1
                            }

                            _state.update {
                                it.copy(
                                    schedTitle = scheduledMeetReceived.title,
                                    schedId = scheduledMeetReceived.schedId,
                                    schedUntil = until,
                                    typeOccurs = freq
                                )
                            }

                            getOccurrences()
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
                            firstParticipant = if (list.isNotEmpty()) list.first() else null,
                            secondParticipant = if (list.size > 1) list[1] else null
                        )
                    }
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Load all occurrences
     */
    private fun getOccurrences() =
        viewModelScope.launch {
            runCatching {
                fetchScheduledMeetingOccurrencesByChat(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Error retrieving list of occurrences: $exception")
            }.onSuccess { list ->
                list?.let { listOccurrences ->
                    Timber.d("List of occurrences successfully retrieved")
                    val newList = state.value.occurrencesList.toMutableList()
                    for (occurrence in listOccurrences) {
                        if (!newList.contains(occurrence)) {
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
     * Control the visibility of the button see more occurrences
     */
    private fun checkSeeMoreVisibility(sizeNewOccurrences: Int) {
        if (sizeNewOccurrences < 20) {
            _state.update {
                it.copy(showSeeMoreButton = false)
            }
            return
        }

        state.value.schedUntil?.let { until ->
            state.value.occurrencesList.last().startDateTime?.let { time ->
                _state.update {
                    it.copy(showSeeMoreButton = time < until)
                }
            }
            return
        }

        _state.update {
            it.copy(showSeeMoreButton = false)
        }
    }

    /**
     * See more occurrences
     */
    fun onSeeMoreOccurrencesTap() {
        Timber.d("Get more occurrences")
        getOccurrences()
    }

    /**
     * Finish activity
     */
    private fun finishActivity() =
        _state.update { state ->
            state.copy(finish = true)
        }
}