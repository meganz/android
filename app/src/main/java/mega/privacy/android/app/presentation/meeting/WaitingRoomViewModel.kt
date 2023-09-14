package mega.privacy.android.app.presentation.meeting

import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.TermCodeType
import mega.privacy.android.domain.entity.meeting.WaitingRoomStatus
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLocalVideoUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.InitGuestChatSessionUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.JoinGuestChatCallUseCase
import mega.privacy.android.domain.usecase.chat.StartVideoDeviceUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.GetScheduleMeetingDataUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.waitingroom.IsValidWaitingRoomUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes


/**
 * Waiting room view model
 *
 * @property monitorConnectivityUseCase
 * @property getScheduleMeetingDataUseCase
 * @property timestampMapper
 * @property getMyAvatarFileUseCase
 * @property getMyAvatarColorUseCase
 * @property getUserFullNameUseCase
 * @property isValidWaitingRoomUseCase
 * @property monitorChatCallUpdates
 * @property monitorScheduledMeetingUpdates
 * @property getChatCall
 * @property getChatLocalVideoUpdatesUseCase
 * @property setChatVideoInDeviceUseCase
 * @property startVideoDeviceUseCase
 * @property answerChatCallUseCase
 * @property initGuestChatSessionUseCase
 * @property joinGuestChatCallUseCase
 * @property checkChatLinkUseCase
 * @property isUserLoggedIn
 * @property isEphemeralPlusPlusUseCase
 * @property logoutUseCase
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaitingRoomViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getScheduleMeetingDataUseCase: GetScheduleMeetingDataUseCase,
    private val timestampMapper: ChatRoomTimestampMapper,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val isValidWaitingRoomUseCase: IsValidWaitingRoomUseCase,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val getChatCall: GetChatCall,
    private val getChatLocalVideoUpdatesUseCase: GetChatLocalVideoUpdatesUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val startVideoDeviceUseCase: StartVideoDeviceUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
    private val joinGuestChatCallUseCase: JoinGuestChatCallUseCase,
    private val checkChatLinkUseCase: CheckChatLinkUseCase,
    private val isUserLoggedIn: IsUserLoggedIn,
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    companion object {
        private const val WAITING_ROOM_TIMEOUT = 10
    }

    private val _state = MutableStateFlow(WaitingRoomState())
    val state: StateFlow<WaitingRoomState> = _state

    init {
        viewModelScope.launch {
            initChatGuestSessionIfNeeded()
            monitorCallUpdates()
            monitorMeetingUpdates()
            setChatVideoDevice()
            retrieveUserAvatar()
            startCountdownTimer()
        }
    }

    /**
     * On ViewModel cleared
     */
    override fun onCleared() {
        enableCamera(false)
        super.onCleared()
    }

    /**
     * Start chat session for guest users if needed
     */
    private suspend fun initChatGuestSessionIfNeeded() {
        runCatching {
            if (!isUserLoggedIn()) {
                initGuestChatSessionUseCase(anonymousMode = true)
            }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Load Meeting details
     *
     * @param chatId        Chat Room Id
     * @param chatLink      Chat Room Link
     */
    fun loadMeetingDetails(chatId: Long?, chatLink: String?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    chatId = chatId ?: -1L,
                    chatLink = chatLink,
                    guestMode = !chatLink.isNullOrBlank() && !isUserLoggedIn(),
                )
            }

            when {
                chatId != null -> {
                    if (isValidWaitingRoom(chatId)) {
                        retrieveMeetingDetails()
                        retrieveCallDetails()
                    } else {
                        _state.update { it.copy(joinCall = true) }
                    }
                }

                !chatLink.isNullOrBlank() -> {
                    if (isValidWaitingRoom(chatLink)) {
                        retrieveChatLinkDetails(chatLink)
                    } else {
                        _state.update { it.copy(joinCall = true) }
                    }
                }

                else -> error("Invalid parameters")
            }
        }
    }

    /**
     * Get SDK video stream flow
     *
     * @return  [Flow] emitting pairs of [Size] and [ByteArray] representing each video frame.
     */
    fun getVideoStream(): Flow<Pair<Size, ByteArray>> =
        getChatLocalVideoUpdatesUseCase()
            .catch { Timber.e(it) }
            .mapLatest { frame -> Size(frame.width, frame.height) to frame.byteBuffer }

    /**
     * Retrieve current Chat Room Scheduled Meeting details
     */
    private fun retrieveMeetingDetails() {
        viewModelScope.launch {
            runCatching {
                getScheduleMeetingDataUseCase(
                    chatId = _state.value.chatId,
                    meetingTimeMapper = timestampMapper::getWaitingRoomTimeFormatted
                )
            }.onSuccess { meeting ->
                _state.update {
                    it.copy(
                        schedId = meeting.schedId,
                        title = meeting.title,
                        formattedTimestamp = meeting.scheduledTimestampFormatted,
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Retrieve current Chat Room call details
     */
    private fun retrieveCallDetails() {
        viewModelScope.launch {
            runCatching {
                getChatCall(chatId = _state.value.chatId)
            }.onSuccess { call ->
                call?.updateUiState()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Retrieve Chat Room link details
     *
     * @param chatLink  Chat Room link
     */
    private fun retrieveChatLinkDetails(chatLink: String) {
        viewModelScope.launch {
            runCatching {
                checkChatLinkUseCase(chatLink).also { requireNotNull(it.chatHandle) }
            }.onSuccess { chatRequest ->
                _state.update {
                    it.copy(
                        chatId = requireNotNull(chatRequest.chatHandle),
                        title = chatRequest.text,
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Monitor current chat call updates
     */
    private fun monitorCallUpdates() {
        viewModelScope.launch {
            monitorChatCallUpdates()
                .filter { it.chatId == _state.value.chatId }
                .distinctUntilChanged()
                .collectLatest { it.updateUiState() }
        }
    }

    /**
     * Monitor current meeting updates
     */
    private fun monitorMeetingUpdates() {
        viewModelScope.launch {
            monitorScheduledMeetingUpdates()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { retrieveMeetingDetails() }
        }
    }

    /**
     * Set chat video In Device
     */
    private fun setChatVideoDevice() =
        viewModelScope.launch {
            runCatching {
                setChatVideoInDeviceUseCase()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Retrieve current user avatar
     */
    private fun retrieveUserAvatar() {
        viewModelScope.launch {
            runCatching {
                if (!isUserLoggedIn() || isEphemeralPlusPlusUseCase()) return@launch

                val avatar = async { getMyAvatarFileUseCase(false)?.absolutePath }
                val color = async { getMyAvatarColorUseCase() }
                val fullName = async { getUserFullNameUseCase(false) }
                ChatAvatarItem(
                    uri = avatar.await(),
                    color = color.await(),
                    placeholderText = fullName.await(),
                )
            }.onSuccess { avatar ->
                _state.update { it.copy(avatar = avatar) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Check if current Chat Room Id is valid waiting room
     *
     * @param chatId    Chat Room id
     * @return          true if it's valid, false otherwise
     */
    private suspend fun isValidWaitingRoom(chatId: Long): Boolean = runCatching {
        isValidWaitingRoomUseCase(chatId)
    }.onFailure(Timber.Forest::e).getOrNull() ?: false

    /**
     * Check if current Chat Room Link is valid waiting room
     *
     * @param chatLink  Chat Room link
     * @return          true if it's valid, false otherwise
     */
    private suspend fun isValidWaitingRoom(chatLink: String): Boolean = runCatching {
        isValidWaitingRoomUseCase(chatLink)
    }.onFailure(Timber.Forest::e).getOrNull() ?: false

    /**
     * Update UI state based on the provided [ChatCall]
     */
    private fun ChatCall.updateUiState() {
        when {
            hasAccessBeenDenied() ->
                _state.update { it.copy(denyAccessDialog = true) }

            hasAccessBeenGranted() ->
                _state.update { it.copy(joinCall = true) }

            hasStarted() -> {
                _state.update { it.copy(callStarted = true) }

                if (shouldBeAnswered()) {
                    answerChatCall()
                }
            }

            else ->
                _state.update { it.copy(callStarted = false) }
        }
    }

    /**
     * Start countdown timer to show inactive dialog after [WAITING_ROOM_TIMEOUT] minutes.
     */
    private fun startCountdownTimer() {
        viewModelScope.launch {
            delay(WAITING_ROOM_TIMEOUT.minutes)
            _state.update { it.copy(inactiveHostDialog = true) }
        }
    }

    /**
     * Check if [ChatCall] access has been granted
     */
    private fun ChatCall.hasAccessBeenGranted(): Boolean =
        changes?.contains(ChatCallChanges.WaitingRoomAllow) == true
                || status == ChatCallStatus.Joining || status == ChatCallStatus.InProgress
                || waitingRoomStatus == WaitingRoomStatus.Allowed

    /**
     * Check if [ChatCall] access has been denied
     */
    private fun ChatCall.hasAccessBeenDenied(): Boolean =
        changes?.contains(ChatCallChanges.WaitingRoomDeny) == true
                || (waitingRoomStatus == WaitingRoomStatus.NotAllowed
                && termCode == TermCodeType.Kicked)

    /**
     * Check if [ChatCall] should be answered
     */
    private fun ChatCall.shouldBeAnswered(): Boolean =
        (status == ChatCallStatus.WaitingRoom || status == ChatCallStatus.UserNoPresent)
                && !_state.value.guestMode

    /**
     * Check if [ChatCall] has started
     */
    private fun ChatCall.hasStarted(): Boolean =
        when (status) {
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.UserNoPresent,
            ChatCallStatus.Connecting,
            ChatCallStatus.Joining,
            ChatCallStatus.InProgress,
            -> true

            else -> false
        }

    private fun answerChatCall() {
        viewModelScope.launch {
            runCatching {
                val state = state.value
                answerChatCallUseCase(state.chatId, state.cameraEnabled, state.micEnabled)
            }.onSuccess { call ->
                call?.updateUiState()
            }.onFailure { error ->
                if (error is MegaException && error.errorCode == MegaChatError.ERROR_EXIST) {
                    // Already requested, do nothing.
                } else {
                    Timber.e(error)
                }
            }
        }
    }

    /**
     * Enable device camera
     *
     * @param enable    true to enable camera, false otherwise
     */
    fun enableCamera(enable: Boolean) =
        viewModelScope.launch {
            runCatching {
                startVideoDeviceUseCase(enable)
            }.onSuccess {
                _state.update { it.copy(cameraEnabled = enable) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Enable device microphone
     *
     * @param enable    true to enable microphone, false otherwise
     */
    fun enableMicrophone(enable: Boolean) {
        _state.update { it.copy(micEnabled = enable) }
    }

    /**
     * Enable device speaker
     *
     * @param enable    true to enable speaker, false otherwise
     */
    fun enableSpeaker(enable: Boolean) {
        _state.update { it.copy(speakerEnabled = enable) }
    }

    /**
     * Set guest user name
     *
     * @param firstName
     * @param lastName
     */
    fun setGuestName(firstName: String, lastName: String) {
        _state.update {
            it.copy(
                guestFirstName = firstName,
                guestLastName = lastName,
                guestMode = false,
            )
        }

        joinCallAsGuest()
    }

    /**
     * Join current call as a guest.
     *
     * Also disables camera during guest login process to avoid SDK issues.
     */
    private fun joinCallAsGuest() {
        viewModelScope.launch {
            val cameraEnabled = _state.value.cameraEnabled
            runCatching {
                if (cameraEnabled) enableCamera(false).join()

                joinGuestChatCallUseCase(
                    chatLink = requireNotNull(_state.value.chatLink),
                    firstName = requireNotNull(_state.value.guestFirstName),
                    lastName = requireNotNull(_state.value.guestLastName),
                )
            }.onSuccess {
                if (cameraEnabled) enableCamera(true)

                setChatVideoDevice()
                retrieveMeetingDetails()
                retrieveCallDetails()
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update { it.copy(guestMode = true) }
            }
        }
    }

    /**
     * Finish current Waiting Room session
     */
    fun finishWaitingRoom() {
        viewModelScope.launch {
            runCatching {
                if (isEphemeralPlusPlusUseCase()) {
                    logoutUseCase()
                }
            }.onSuccess {
                _state.update { it.copy(finish = true) }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }
}
