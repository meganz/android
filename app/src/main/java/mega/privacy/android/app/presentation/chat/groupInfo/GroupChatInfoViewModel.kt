package mega.privacy.android.app.presentation.chat.groupInfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.flow.StateFlow
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
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.openMeetingWithAudioOrVideo
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.StartChatCall
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject

/**
 * GroupChatInfoActivity view model.
 *
 * @property setOpenInvite               [SetOpenInvite]
 * @property startChatCall               [StartChatCall]
 * @property getChatRoomUseCase          [GetChatRoomUseCase]
 * @property passcodeManagement          [PasscodeManagement]
 * @property chatApiGateway              [MegaChatApiGateway]
 * @property cameraGateway               [CameraGateway]
 * @property chatManagement              [ChatManagement]
 * @property state                       Current view state as [GroupInfoState]
 */
@HiltViewModel
class GroupChatInfoViewModel @Inject constructor(
    private val setOpenInvite: SetOpenInvite,
    monitorConnectivity: MonitorConnectivity,
    private val startChatCall: StartChatCall,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
) : BaseRxViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(GroupInfoState())

    /**
     * public UI State
     */
    val state: StateFlow<GroupInfoState> = _state

    private val isConnected =
        monitorConnectivity().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val openInviteChangeObserver =
        Observer<MegaChatRoom> { chat ->
            _state.update {
                it.copy(resultSetOpenInvite = chat.isOpenInvite)
            }
        }

    init {
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .observeForever(openInviteChangeObserver)
    }

    override fun onCleared() {
        super.onCleared()
        LiveEventBus.get(EventConstants.EVENT_CHAT_OPEN_INVITE, MegaChatRoom::class.java)
            .removeObserver(openInviteChangeObserver)
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
     * Get chat id
     *
     * @param userHandle User handle
     */
    fun getChatRoomId(userHandle: Long): LiveData<Long> {
        val result = MutableLiveData<Long>()
        getChatRoomUseCase.get(userHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { chatId ->
                    result.value = chatId
                },
                onError = Timber::e
            )
            .addTo(composite)
        return result
    }

    /**
     * Starts a call
     *
     * @param chatId Chat id
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    fun onCallTap(chatId: Long, video: Boolean, audio: Boolean) {
        if (chatApiGateway.getChatCall(chatId) != null) {
            Timber.d("There is a call, open it")
            CallUtil.openMeetingInProgress(MegaApplication.getInstance().applicationContext,
                chatId,
                true,
                passcodeManagement)
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

                    openMeetingWithAudioOrVideo(MegaApplication.getInstance().applicationContext,
                        resultChatId,
                        audioEnable,
                        videoEnable,
                        passcodeManagement)
                }
            }
        }
    }
}