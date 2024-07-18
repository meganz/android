package mega.privacy.android.app.contacts.list

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
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
import mega.privacy.android.app.contacts.mapper.ContactItemDataMapper
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.domain.usecase.contact.RemoveContactByEmailUseCase
import mega.privacy.android.domain.usecase.call.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that handles all related logic to Contact List for the current user.
 *
 * @property getContactsUseCase Use case to get contacts.
 * @property get1On1ChatIdUseCase Use case to get 1 on 1 chat id.
 * @property removedContactByEmailUseCase Use case to remove contact by email.
 * @property startCallUseCase Use case to start a call.
 * @property passcodeManagement PasscodeManagement object.
 * @property chatApiGateway MegaChatApiGateway object.
 * @property setChatVideoInDeviceUseCase Use case to set chat video in device.
 * @property chatManagement ChatManagement object.
 * @property createShareKeyUseCase Use case to create a share key.
 * @property getNodeByIdUseCase Use case to get a node by id.
 * @property monitorSFUServerUpgradeUseCase Use case to monitor SFU server upgrade.
 * @property monitorContactRequestsUseCase Use case to monitor contact requests.
 * @property contactItemDataMapper Mapper to map ContactItem to ContactItem.Data.
 * @property context Application context.
 */
@HiltViewModel
internal class ContactListViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val removedContactByEmailUseCase: RemoveContactByEmailUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val chatApiGateway: MegaChatApiGateway,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val monitorSFUServerUpgradeUseCase: MonitorSFUServerUpgradeUseCase,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
    private val contactItemDataMapper: ContactItemDataMapper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val queryString = MutableStateFlow<String?>(null)
    private val contacts: MutableStateFlow<List<ContactItem>> = MutableStateFlow(emptyList())
    private val _state = MutableStateFlow(ContactListState())

    private var monitorSFUServerUpgradeJob: Job? = null

    /**
     * State of the UI for the contact list screen.
     */
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            combine(
                getContactsUseCase().map { domainList: List<mega.privacy.android.domain.entity.contacts.ContactItem> ->
                    domainList.map { contactItemDataMapper(it) }.sortedAlphabetically()
                },
                queryString
            ) { list: List<ContactItem.Data>, query: String? ->
                if (query.isNullOrBlank()) {
                    list.groupBy { it.getFirstCharacter() }
                        .flatMap { (header, list) ->
                            mutableListOf<ContactItem>().apply {
                                add(ContactItem.Header(header))
                                addAll(list)
                            }
                        }
                } else {
                    list.filter { it.matches(query) }
                }
            }.distinctUntilChanged()
                .collectLatest {
                    contacts.value = it
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

    private fun List<ContactItem.Data>.sortedAlphabetically(): List<ContactItem.Data> =
        sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, ContactItem.Data::getTitle))

    fun getRecentlyAddedContacts(): LiveData<List<ContactItem>> =
        combine(
            queryString,
            contacts
                .map { list ->
                    list.filterIsInstance<ContactItem.Data>()
                        .filter { it.isNew }
                }
        ) { query, newContacts ->
            if (query.isNullOrBlank()) {
                mutableListOf<ContactItem>().apply {
                    add(ContactItem.Header(context.getString(R.string.section_recently_added)))
                    addAll(newContacts)
                    add(ContactItem.Header(context.getString(R.string.section_contacts)))
                }
            } else {
                emptyList()
            }
        }.asLiveData()

    fun getContactsWithHeaders(): LiveData<List<ContactItem>> =
        contacts.asLiveData()

    fun getContact(userHandle: Long): LiveData<ContactItem.Data?> =
        contacts.map { contact -> contact.find { it is ContactItem.Data && it.handle == userHandle } }
            .filterIsInstance<ContactItem.Data>()
            .asLiveData()

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
     */
    fun removeContact(userEmail: String) {
        viewModelScope.launch {
            runCatching {
                removedContactByEmailUseCase(userEmail)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun setQuery(query: String?) {
        queryString.value = query
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
                startCallUseCase(chatId = chatId, audio = true, video = video)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { call ->
                call?.apply {
                    CallUtil.addChecksForACall(chatId, hasLocalVideo)
                    if (isOutgoing) {
                        chatManagement.setRequestSentCall(call.callId, isRequestSent = true)
                    }

                    CallUtil.openMeetingWithAudioOrVideo(
                        MegaApplication.getInstance().applicationContext,
                        chatId,
                        hasLocalAudio,
                        hasLocalVideo,
                        passcodeManagement
                    )
                }
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

    fun getContactEmail(userHandle: Long) =
        getContact(userHandle).map { it?.email }
}
