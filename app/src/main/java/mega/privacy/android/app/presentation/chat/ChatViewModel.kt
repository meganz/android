package mega.privacy.android.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.model.ChatState
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.AnswerChatCall
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.megachat.ChatActivity]
 *
 * @property answerChatCall             [AnswerChatCall]
 * @property passcodeManagement         [PasscodeManagement]
 * @property cameraGateway              [CameraGateway]
 * @property chatManagement             [ChatManagement]
 * @property megaChatApiGateway         [MegaChatApiGateway]
 * @property rtcAudioManagerGateway     [RTCAudioManagerGateway]
 * @property isConnected True if the app has some network connection, false otherwise.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    monitorConnectivity: MonitorConnectivity,
    private val answerChatCall: AnswerChatCall,
    private val passcodeManagement: PasscodeManagement,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val megaChatApiGateway: MegaChatApiGateway
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ChatState())

    /**
     * public UI State
     */
    val state: StateFlow<ChatState> = _state

    /**
     * Get latest [StorageState] from [MonitorStorageStateEvent] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false).value

    /**
     * Answers a call.
     *
     * @param chatId
     * @param video True, video on. False, video off.
     * @param audio True, audio on. False, video off.
     * @param speaker True, speaker on. False, speaker off.
     */
    fun onAnswerCall(chatId: Long, video: Boolean, audio: Boolean, speaker: Boolean) {
        if (CallUtil.amIParticipatingInThisMeeting(chatId)) {
            Timber.d("Already participating in this call")
            _state.update { it.copy(isCallAnswered = true) }
            return
        }

        if (MegaApplication.getChatManagement().isAlreadyJoiningCall(chatId)) {
            Timber.d("The call has been answered")
            _state.update { it.copy(isCallAnswered = true) }
            return
        }

        cameraGateway.setFrontCamera()
        chatManagement.addJoiningCallChatId(chatId)

        viewModelScope.launch {
            runCatching {
                answerChatCall(chatId, video, audio, speaker)
            }.onFailure { exception ->
                _state.update { it.copy(error = R.string.call_error) }

                chatManagement.removeJoiningCallChatId(chatId)
                rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                megaChatApiGateway.getChatCall(chatId)?.let { call ->
                    CallUtil.clearIncomingCallNotification(call.callId)
                }
                Timber.e(exception)
            }.onSuccess { resultAnswerCall ->
                val resultChatId = resultAnswerCall.chatHandle
                if (resultChatId != null) {
                    val videoEnable = resultAnswerCall.flag

                    _state.update { it.copy(isCallAnswered = true) }

                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    chatManagement.setSpeakerStatus(chatId, videoEnable)

                    megaChatApiGateway.getChatCall(chatId)?.let { call ->
                        CallUtil.clearIncomingCallNotification(call.callId)
                        chatManagement.setRequestSentCall(call.callId, false)
                    }

                    CallUtil.openMeetingInProgress(MegaApplication.getInstance().applicationContext,
                        resultChatId,
                        true,
                        passcodeManagement)
                }
            }
        }
    }
}