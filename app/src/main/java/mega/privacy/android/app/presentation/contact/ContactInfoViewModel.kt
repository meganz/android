package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.contact.model.ContactInfoState
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.StartChatCall
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactInfoActivity]
 *
 * @property monitorStorageStateEvent [MonitorStorageStateEvent]
 * @property monitorConnectivity      [MonitorConnectivity]
 * @property startChatCall            [StartChatCall]
 * @property getChatRoomUseCase       [GetChatRoomUseCase]
 * @property passcodeManagement       [PasscodeManagement]
 * @property chatApiGateway           [MegaChatApiGateway]
 * @property cameraGateway            [CameraGateway]
 * @property chatManagement           [ChatManagement]
 * @property areCredentialsVerified   [AreCredentialsVerified]
 * @property monitorContactUpdates    [MonitorContactUpdates]
 */
@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorConnectivity: MonitorConnectivity,
    private val startChatCall: StartChatCall,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val areCredentialsVerified: AreCredentialsVerified,
    private val monitorContactUpdates: MonitorContactUpdates,
) : BaseRxViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ContactInfoState())

    /**
     * public UI State
     */
    val state: StateFlow<ContactInfoState> = _state

    init {
        getContactUpdates()
    }

    /**
     * Sets the initial data of the contact.
     *
     * @param email The contact's email.
     */
    fun setupData(handle: Long, email: String) {
        _state.update { it.copy(userId = UserId(handle), email = email) }
        checkCredentials()
    }

    private fun checkCredentials() = state.value.email?.let { email ->
        viewModelScope.launch {
            _state.update { it.copy(areCredentialsVerified = areCredentialsVerified(email)) }
        }
    }

    /**
     * Monitors contact changes.
     */
    private fun getContactUpdates() {
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { updates ->
                val contactUpdates = updates.changes.values.map {
                    it.contains(UserChanges.AuthenticationInformation)
                }

                if (contactUpdates.isNotEmpty()) {
                    checkCredentials()
                }
            }
        }
    }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEvent] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = monitorConnectivity().value

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
            _state.update { it.copy(isCallStarted = true) }

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
                _state.update { it.copy(error = R.string.call_error) }
                Timber.e(exception)
            }.onSuccess { resultStartCall ->
                _state.update { it.copy(isCallStarted = true) }
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

                    CallUtil.openMeetingWithAudioOrVideo(MegaApplication.getInstance().applicationContext,
                        resultChatId,
                        audioEnable,
                        videoEnable,
                        passcodeManagement)
                }
            }
        }
    }
}