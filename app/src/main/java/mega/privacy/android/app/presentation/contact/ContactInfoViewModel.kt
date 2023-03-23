package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
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
import mega.privacy.android.app.presentation.extensions.isAwayOrOffline
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomByUser
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.contact.ApplyContactUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmail
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandle
import mega.privacy.android.domain.usecase.contact.SetUserAlias
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactInfoActivity]
 *
 * @property monitorStorageStateEventUseCase    [MonitorStorageStateEventUseCase]
 * @property monitorConnectivityUseCase         [MonitorConnectivityUseCase]
 * @property startChatCall                      [StartChatCall]
 * @property getChatRoomUseCase                 [GetChatRoomUseCase]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property chatApiGateway                     [MegaChatApiGateway]
 * @property cameraGateway                      [CameraGateway]
 * @property chatManagement                     [ChatManagement]
 * @property monitorContactUpdates              [MonitorContactUpdates]
 * @property requestLastGreen                   [RequestLastGreen]
 * @property getChatRoom                        [GetChatRoom]
 * @property getUserOnlineStatusByHandle        [GetUserOnlineStatusByHandle]
 * @property getChatRoomByUser                  [GetChatRoomByUser]
 * @property getContactFromChatUseCase          [GetContactFromChatUseCase]
 * @property getContactFromEmail                [GetContactFromEmail]
 * @property applyContactUpdatesUseCase         [ApplyContactUpdatesUseCase]
 * @property setUserAlias                       [SetUserAlias]
 */
@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val startChatCall: StartChatCall,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val getUserOnlineStatusByHandle: GetUserOnlineStatusByHandle,
    private val requestLastGreen: RequestLastGreen,
    private val getChatRoom: GetChatRoom,
    private val getChatRoomByUser: GetChatRoomByUser,
    private val getContactFromChatUseCase: GetContactFromChatUseCase,
    private val getContactFromEmail: GetContactFromEmail,
    private val applyContactUpdatesUseCase: ApplyContactUpdatesUseCase,
    private val setUserAlias: SetUserAlias,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseRxViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ContactInfoState())

    /**
     * public UI State
     */
    val state: StateFlow<ContactInfoState> = _state

    /**
     * Parent Handle
     */
    var parentHandle: Long? = null

    /**
     * Checks if contact info launched from contacts screen
     */
    val isFromContacts: Boolean
        get() = state.value.isFromContacts

    /**
     * User status
     */
    val userStatus: UserStatus
        get() = state.value.userStatus


    /**
     * Nick name
     */
    val nickName: String?
        get() = state.value.contactItem?.contactData?.alias

    init {
        getContactUpdates()
    }

    /**
     * Monitors contact changes.
     */
    private fun getContactUpdates() = viewModelScope.launch {
        monitorContactUpdates().collectLatest { updates ->
            val userInfo = state.value.contactItem?.let { applyContactUpdatesUseCase(it, updates) }
            if (updates.changes.containsValue(listOf(UserChanges.Avatar))) {
                updateAvatar(userInfo)
            } else {
                _state.update { it.copy(contactItem = userInfo) }
            }
        }
    }

    /**
     * Refreshes user info
     *
     * User online status, Credential verification, User info change will be updated
     */
    fun refreshUserInfo() = viewModelScope.launch {
        val email = state.value.email ?: return@launch
        runCatching {
            getContactFromEmail(email, isOnline())
        }.onSuccess {
            _state.update { state -> state.copy(contactItem = it) }
            it?.handle?.let { handle -> runCatching { requestLastGreen(handle) } }
        }
    }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = monitorConnectivityUseCase().value

    /**
     * Get chat id
     *
     * @param userHandle User handle
     */
    fun getChatRoomId(userHandle: Long): LiveData<Long> {
        val result = MutableLiveData<Long>()
        getChatRoomUseCase.get(userHandle).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = { chatId ->
                    result.value = chatId
                }, onError = Timber::e
            ).addTo(composite)
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

                    CallUtil.openMeetingWithAudioOrVideo(
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
     * Gets user online status by user handle
     * Requests for lastGreen
     */
    fun getUserStatusAndRequestForLastGreen() = viewModelScope.launch {
        val handle = state.value.contactItem?.handle ?: return@launch
        runCatching { getUserOnlineStatusByHandle(handle) }.onSuccess { status ->
            if (status.isAwayOrOffline()) {
                requestLastGreen(handle)
            }
            _state.update {
                it.copy(userStatus = status)
            }
        }
    }

    /**
     * Method updates the last green status to contact info state
     *
     * @param userHandle user handle of the user
     * @param lastGreen last green status
     */
    fun updateLastGreen(userHandle: Long, lastGreen: Int) = viewModelScope.launch {
        runCatching {
            getUserOnlineStatusByHandle(userHandle = userHandle)
        }.onSuccess { status ->
            _state.update {
                it.copy(
                    lastGreen = lastGreen,
                    userStatus = status,
                )
            }
        }
    }

    /**
     * Method to get chat room and contact info
     *
     * @param chatHandle chat handle of selected chat
     * @param email email id of selected user
     */
    fun updateContactInfo(chatHandle: Long, email: String? = null) = viewModelScope.launch {
        val isFromContacts = chatHandle == -1L
        var userInfo: ContactItem? = null
        var chatRoom: ChatRoom? = null
        if (isFromContacts) {
            runCatching {
                email?.let { mail -> getContactFromEmail(mail, isOnline()) }
            }.onSuccess { contact ->
                userInfo = contact
                chatRoom = runCatching {
                    contact?.handle?.let { getChatRoomByUser(it) }
                }.getOrNull()
            }
        } else {
            runCatching {
                chatRoom = getChatRoom(chatHandle)
                userInfo = getContactFromChatUseCase(chatHandle, isOnline())
            }
        }

        _state.update {
            it.copy(
                isFromContacts = isFromContacts,
                lastGreen = userInfo?.lastSeen ?: 0,
                userStatus = userInfo?.status ?: UserStatus.Invalid,
                contactItem = userInfo,
                chatRoom = chatRoom,
            )
        }
        updateAvatar(userInfo)
    }

    private fun updateAvatar(userInfo: ContactItem?) = viewModelScope.launch(ioDispatcher) {
        val avatarFile = userInfo?.contactData?.avatarUri?.let { File(it) }
        val avatar = AvatarUtil.getAvatarBitmap(avatarFile)
        _state.update { it.copy(avatar = avatar) }
    }

    /**
     * Sets parent handle
     *
     * @param handle parent handle
     */
    fun setParentHandle(handle: Long) {
        parentHandle = handle
    }

    /**
     * Method to update the nick name of the user
     *
     * @param newNickname new nick name given by the user
     */
    fun updateNickName(newNickname: String?) = viewModelScope.launch {
        val handle = state.value.contactItem?.handle
        if (handle == null || (nickName != null && nickName == newNickname)) return@launch
        runCatching {
            setUserAlias(newNickname, handle)
        }.onSuccess { alias ->
            _state.update {
                it.copy(
                    snackBarMessage = alias?.let { R.string.snackbar_nickname_added }
                        ?: run { R.string.snackbar_nickname_removed },
                )
            }
        }
    }

    /**
     * on Consume Snack Bar Message event
     */
    fun onConsumeSnackBarMessageEvent() {
        viewModelScope.launch {
            _state.update { it.copy(snackBarMessage = null) }
        }
    }
}