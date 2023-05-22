package mega.privacy.android.app.presentation.chat.groupInfo

import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.groupInfo.model.GroupInfoState
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.openMeetingWithAudioOrVideo
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * GroupChatInfoActivity view model.
 *
 * @property setOpenInvite                                  [SetOpenInvite]
 * @property startChatCall                                  [StartChatCall]
 * @property getChatRoomUseCase                             [GetChatRoomUseCase]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property chatApiGateway                                 [MegaChatApiGateway]
 * @property cameraGateway                                  [CameraGateway]
 * @property chatManagement                                 [ChatManagement]
 * @property endCallUseCase                                 [EndCallUseCase]
 * @property sendStatisticsMeetingsUseCase                  [SendStatisticsMeetingsUseCase]
 * @property monitorUpdatePushNotificationSettingsUseCase   [MonitorUpdatePushNotificationSettingsUseCase]
 * @property broadcastChatArchivedUseCase                   [BroadcastChatArchivedUseCase]
 * @property state                                          Current view state as [GroupInfoState]
 */
@HiltViewModel
class GroupChatInfoViewModel @Inject constructor(
    private val setOpenInvite: SetOpenInvite,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val startChatCall: StartChatCall,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase,
) : BaseRxViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(GroupInfoState())

    /**
     * UI State GroupChatInfo
     * Flow of [GroupInfoState]
     */
    val state = _state.asStateFlow()

    private val isConnected =
        monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val openInviteChangeObserver =
        Observer<MegaChatRoom> { chat ->
            _state.update {
                it.copy(resultSetOpenInvite = chat.isOpenInvite)
            }
        }

    init {
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .observeForever(openInviteChangeObserver)
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .removeObserver(openInviteChangeObserver)
    }

    /**
     * Sets chat id
     *
     * @param newChatId   Chat id.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != chatApiGateway.getChatInvalidHandle() && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
        }
    }

    /**
     * Allow add participants
     */
    fun onAllowAddParticipantsTap(chatId: Long) {
        if (isConnected.value) {
            viewModelScope.launch {
                runCatching {
                    setOpenInvite(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(error = R.string.general_text_error) }
                }.onSuccess { result ->
                    _state.update {
                        it.copy(resultSetOpenInvite = result)
                    }
                }
            }
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Method for processing when clicking on the call option
     *
     * @param userHandle Use handle
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    fun onCallTap(userHandle: Long, video: Boolean, audio: Boolean) {
        getChatRoomUseCase.get(userHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { chatId ->
                    startCall(chatId, video, audio)
                },
                onError = Timber::e
            )
            .addTo(composite)
    }

    /**
     * Starts a call
     *
     * @param chatId Chat id
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    private fun startCall(chatId: Long, video: Boolean, audio: Boolean) {
        if (chatApiGateway.getChatCall(chatId) != null) {
            Timber.d("There is a call, open it")
            CallUtil.openMeetingInProgress(
                MegaApplication.getInstance().applicationContext,
                chatId,
                true,
                passcodeManagement
            )
            return
        }

        MegaApplication.isWaitingForCall = false

        cameraGateway.setFrontCamera()

        viewModelScope.launch {
            runCatching {
                startChatCall(chatId, video, audio)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { resultStartCall ->
                val resultChatId = resultStartCall.chatHandle
                if (resultChatId != null) {
                    val videoEnable = resultStartCall.flag
                    val paramType = resultStartCall.paramType
                    val audioEnable: Boolean = paramType == ChatRequestParamType.Video

                    CallUtil.addChecksForACall(resultChatId, videoEnable)

                    chatApiGateway.getChatCall(resultChatId)?.let { call ->
                        if (call.isOutgoing) {
                            chatManagement.setRequestSentCall(call.callId, true)
                        }
                    }

                    openMeetingWithAudioOrVideo(
                        MegaApplication.getInstance().applicationContext,
                        resultChatId,
                        audioEnable,
                        videoEnable,
                        passcodeManagement
                    )
                }
            }
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() {
        endCallUseCase.endCallForAllWithChatId(_state.value.chatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { error ->
                Timber.e(error.stackTraceToString())
            })
            .addTo(composite)

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = false) }
        }
    }

    /**
     * Launch broadcast for a chat archived event
     *
     * @param chatTitle [String]
     */
    fun launchBroadcastChatArchived(chatTitle: String) = viewModelScope.launch {
        broadcastChatArchivedUseCase(chatTitle)
    }
}