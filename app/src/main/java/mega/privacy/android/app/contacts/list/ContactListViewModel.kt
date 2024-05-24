package mega.privacy.android.app.contacts.list

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.contacts.list.data.ContactActionItem
import mega.privacy.android.app.contacts.list.data.ContactActionItem.Type
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.contacts.list.data.ContactListState
import mega.privacy.android.app.contacts.usecase.GetContactsUseCase
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel that handles all related logic to Contact List for the current user.
 *
 * @param getContactsUseCase            Use case to get contacts
 * @param get1On1ChatIdUseCase          Use case to get 1on1 chat id
 * @param removedContactByEmailUseCase  Use case to remove contact by email
 * @param startChatCall                 Use case to start chat call
 * @param passcodeManagement            PasscodeManagement object
 * @param chatApiGateway                MegaChatApiGateway object
 * @param setChatVideoInDeviceUseCase   Use case to set chat video in device
 * @param chatManagement                ChatManagement object
 * @param createShareKeyUseCase         Use case to create share key
 * @param getNodeByIdUseCase            Use case to get node by id
 * @param monitorSFUServerUpgradeUseCase Use case to monitor SFU server upgrade
 * @param monitorContactRequestsUseCase Use case to monitor contact requests
 * @param context                       Application context
 */
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val removedContactByEmailUseCase: RemoveContactByEmailUseCase,
    private val startChatCall: StartChatCall,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val monitorSFUServerUpgradeUseCase: MonitorSFUServerUpgradeUseCase,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val composite = CompositeDisposable()

    companion object {
        private const val REQUEST_TIMEOUT_IN_MS = 100L
    }

    private var queryString: String? = null
    private val contacts: MutableLiveData<List<ContactItem.Data>> = MutableLiveData()
    private val _state = MutableStateFlow(ContactListState())

    private var monitorSFUServerUpgradeJob: Job? = null

    /**
     * State of the UI for the contact list screen.
     */
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            CacheFolderManager.getCacheFolderAsync(CacheFolderManager.AVATAR_FOLDER)?.let {
                retrieveContacts(it)
            }
        }
        retrieveContactActions()
    }

    private fun retrieveContactActions() {
        viewModelScope.launch {
            runCatching {
                monitorContactRequestsUseCase()
                    .map { it.incomingContactRequests.size }
                    .catch { Timber.e(it) }
                    .collectLatest {
                        _state.update { state ->
                            state.copy(
                                contactActionItems = listOf(
                                    ContactActionItem(
                                        Type.REQUESTS,
                                        context.getString(R.string.section_requests),
                                        it
                                    ),
                                    ContactActionItem(
                                        Type.GROUPS,
                                        context.getString(R.string.section_groups)
                                    )
                                )
                            )
                        }
                    }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun retrieveContacts(avatarFolder: File) {
        getContactsUseCase.get(avatarFolder)
            .publish {
                it.take(1).concatWith(it.debounce(REQUEST_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    contacts.value = items.toList()
                },
                onError = Timber::e
            )
            .addTo(composite)
    }


    fun getRecentlyAddedContacts(): LiveData<List<ContactItem>> =
        contacts.map { items ->
            if (queryString.isNullOrBlank() && items.any { it.isNew }) {
                mutableListOf<ContactItem>().apply {
                    add(ContactItem.Header(context.getString(R.string.section_recently_added)))
                    addAll(items.filter { it.isNew })
                    add(ContactItem.Header(context.getString(R.string.section_contacts)))
                }
            } else {
                emptyList()
            }
        }

    fun getContactsWithHeaders(): LiveData<List<ContactItem>> =
        contacts.map { items ->
            val itemsWithHeaders = mutableListOf<ContactItem>()
            items?.forEachIndexed { index, item ->
                if (queryString.isNullOrBlank()) {
                    if (index == 0 || !items[index - 1].getFirstCharacter()
                            .equals(items[index].getFirstCharacter(), true)
                    ) {
                        itemsWithHeaders.add(ContactItem.Header(item.getFirstCharacter()))
                    }
                    itemsWithHeaders.add(item)
                } else if (item.matches(queryString!!)) {
                    itemsWithHeaders.add(item)
                }
            }
            itemsWithHeaders
        }

    fun getContact(userHandle: Long): LiveData<ContactItem.Data?> =
        contacts.map { contact -> contact.find { it.handle == userHandle } }

    fun getMegaUser(userHandle: Long): LiveData<MegaUser> =
        getContact(userHandle).switchMap { user ->
            val result = MutableLiveData<MegaUser>()
            getContactsUseCase.getMegaUser(user?.email)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { megaUser ->
                        result.value = megaUser
                    },
                    onError = Timber::e
                )
                .addTo(composite)
            result
        }

    /**
     * Get chat room ID
     *
     * @param userHandle User handle
     */
    fun getChatRoomId(userHandle: Long) = viewModelScope.launch {
        runCatching {
            get1On1ChatIdUseCase(userHandle)
        }.onSuccess { chatId ->
            _state.update {
                it.copy(
                    shouldOpenChatWithId = chatId
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Reset chat navigation state
     */
    fun onChatOpened() {
        _state.update {
            it.copy(
                shouldOpenChatWithId = null
            )
        }
    }

    /**
     * Remove contact
     *
     * @param megaUser MegaUser to be removed
     */
    fun removeContact(megaUser: MegaUser) {
        viewModelScope.launch {
            runCatching {
                removedContactByEmailUseCase(megaUser.email)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun setQuery(query: String?) {
        queryString = query
        contacts.notifyObserver()
    }

    /**
     * Method for processing when clicking on the call option
     *
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    fun onCallTap(video: Boolean, audio: Boolean) = viewModelScope.launch {
        runCatching {
            get1On1ChatIdUseCase(MegaApplication.userWaitingForCall)
        }.onSuccess { chatId ->
            startCall(chatId, video, audio)
        }.onFailure {
            Timber.e(it)
        }
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

        viewModelScope.launch {
            runCatching {
                setChatVideoInDeviceUseCase()
                startChatCall(chatId, video, audio)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { resultStartCall ->
                val resultChatId = resultStartCall.chatHandle
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

    /**
     * monitor chat call updates
     */
    fun monitorSFUServerUpgrade() {
        monitorSFUServerUpgradeJob?.cancel()
        monitorSFUServerUpgradeJob = viewModelScope.launch {
            monitorSFUServerUpgradeUseCase()
                .catch {
                    Timber.e(it)
                }
                .collect { shouldUpgrade ->
                    if (shouldUpgrade) {
                        showForceUpdateDialog()
                    }
                }
        }
    }

    /**
     * Cancel monitor SFUServerUpgrade
     */
    fun cancelMonitorSFUServerUpgrade() {
        monitorSFUServerUpgradeJob?.cancel()
    }

    private fun showForceUpdateDialog() {
        _state.update { it.copy(showForceUpdateDialog = true) }
    }

    /**
     * Set to false to hide the dialog
     */
    fun onForceUpdateDialogDismissed() {
        _state.update { it.copy(showForceUpdateDialog = false) }
    }

    /**
     * Init share key
     *
     * @param node
     */
    suspend fun initShareKey(node: MegaNode) = runCatching {
        val typedNode = getNodeByIdUseCase(NodeId(node.handle))
        require(typedNode is FolderNode) { "Cannot create a share key for a non-folder node" }
        createShareKeyUseCase(typedNode)
    }.onFailure {
        Timber.e(it)
    }

    override fun onCleared() {
        super.onCleared()
        composite.clear()
    }
}
