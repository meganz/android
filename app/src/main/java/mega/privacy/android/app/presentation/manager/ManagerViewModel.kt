package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.SendStatisticsMediaDiscovery
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel associated to ManagerActivity
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param monitorGlobalUpdates Monitor global updates
 * @param getRubbishBinChildrenNode Fetch the rubbish bin nodes
 * @param getBrowserChildrenNode Fetch the browser nodes
 * @param monitorContactRequestUpdates
 * @param getInboxNode
 * @param getRootFolder Fetch the root node
 * @param getNumUnreadUserAlerts
 * @param hasInboxChildren
 * @param sendStatisticsMediaDiscovery
 * @param savedStateHandle
 * @param ioDispatcher
 * @param monitorMyAvatarFile
 * @param monitorStorageStateEvent monitor global storage state changes
 * @param getCloudSortOrder
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val monitorGlobalUpdates: MonitorGlobalUpdates,
    private val getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    private val getBrowserChildrenNode: GetBrowserChildrenNode,
    monitorContactRequestUpdates: MonitorContactRequestUpdates,
    private val getInboxNode: GetInboxNode,
    private val getRootFolder: GetRootFolder,
    private val getNumUnreadUserAlerts: GetNumUnreadUserAlerts,
    private val hasInboxChildren: HasInboxChildren,
    private val sendStatisticsMediaDiscovery: SendStatisticsMediaDiscovery,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val getPrimarySyncHandle: GetPrimarySyncHandle,
    private val getSecondarySyncHandle: GetSecondarySyncHandle,
    private val checkCameraUpload: CheckCameraUpload,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorConnectivity: MonitorConnectivity,
    private val broadcastUploadPauseState: BroadcastUploadPauseState,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ManagerState())

    /**
     * public UI State
     */
    val state: StateFlow<ManagerState> = _state

    internal val isFirstLoginKey = "EXTRA_FIRST_LOGIN"

    /**
     * private Inbox Node
     */
    private var inboxNode: MegaNode? = null

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.Eagerly)

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    private val isFirstLogin = savedStateHandle.getStateFlow(
        viewModelScope,
        isFirstLoginKey,
        false
    )

    init {
        viewModelScope.launch {
            monitorNodeUpdates().collect {
                checkItemForInbox(it)
                onReceiveNodeUpdate(true)
                checkCameraUploadFolder(false, it)
            }
        }
        viewModelScope.launch(ioDispatcher) {
            isFirstLogin.map {
                { state: ManagerState -> state.copy(isFirstLogin = it) }
            }.collect {
                _state.update(it)
            }
        }
    }

    /**
     * Monitor all global updates
     */
    @Suppress("DEPRECATION")
    private val _updates = monitorGlobalUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor global node updates
     */
    private val _updateNodes = monitorNodeUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor contact requests
     */
    private val _updateContactRequests = monitorContactRequestUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor user updates and dispatch to observers
     */
    val updateUsers: LiveData<Event<List<MegaUser>>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .also { Timber.d("onUsersUpdate") }
            .mapNotNull { it.users?.toList() }
            .map { Event(it) }
            .asLiveData(timeoutInMs = Long.MAX_VALUE)

    /**
     * Monitor user alerts updates and dispatch to observers
     */
    val updateUserAlerts: LiveData<Event<List<MegaUserAlert>>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
            .also { Timber.d("onUserAlertsUpdate") }
            .mapNotNull { it.userAlerts?.toList() }
            .map { Event(it) }
            .asLiveData()

    private fun checkItemForInbox(updatedNodes: List<Node>) {
        //Verify is it is a new item to the inbox
        inboxNode?.let { node ->
            updatedNodes.find { node.handle == it.parentId.id }
                ?.run { updateInboxSectionVisibility() }
        }
    }

    /**
     * Monitor contact request updates and dispatch to observers
     */
    val updateContactsRequests: LiveData<Event<List<ContactRequest>>> =
        _updateContactRequests
            .also { Timber.d("onContactRequestsUpdate") }
            .map { Event(it) }
            .asLiveData()

    /**
     * Update Rubbish Nodes when a node update callback happens
     */
    val updateRubbishBinNodes: LiveData<Event<List<MegaNode>>> =
        _updateNodes
            .also { Timber.d("onRubbishNodesUpdate") }
            .mapNotNull { getRubbishBinChildrenNode(_state.value.rubbishBinParentHandle) }
            .map { Event(it) }
            .asLiveData()

    /**
     * Update Browser Nodes when a node update callback happens
     */
    val updateBrowserNodes: LiveData<Event<List<MegaNode>>> =
        _updateNodes
            .also { Timber.d("onBrowserNodesUpdate") }
            .mapNotNull { getBrowserChildrenNode(_state.value.browserParentHandle) }
            .map { Event(it) }
            .asLiveData()

    /**
     * On my avatar file changed
     */
    val onMyAvatarFileChanged: Flow<File?>
        get() = monitorMyAvatarFile()

    /**
     * Get the browser parent handle
     * If not previously set, set the browser parent handle to root handle
     *
     * @return the handle of the browser section
     */
    fun getSafeBrowserParentHandle(): Long = runBlocking {
        if (_state.value.browserParentHandle == -1L) {
            setBrowserParentHandle(getRootFolder()?.handle ?: INVALID_HANDLE)
        }
        return@runBlocking _state.value.browserParentHandle
    }

    /**
     * Set the current browser parent handle to the UI state
     *
     * @param handle the id of the current browser parent handle to set
     */
    fun setBrowserParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(browserParentHandle = handle) }
    }

    /**
     * Set the current rubbish bin parent handle to the UI state
     *
     * @param handle the id of the current rubbish bin parent handle to set
     */
    fun setRubbishBinParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(rubbishBinParentHandle = handle) }
    }

    /**
     * Set a flag to know if the current navigation level is the first one
     *
     * @param isFirstNavigationLevel true if the current navigation level corresponds to the first level
     */
    fun setIsFirstNavigationLevel(isFirstNavigationLevel: Boolean) = viewModelScope.launch {
        _state.update { it.copy(isFirstNavigationLevel = isFirstNavigationLevel) }
    }

    /**
     * Set the current shares tab to the UI state
     *
     * @param tab shares tab to set
     */
    fun setSharesTab(tab: SharesTab) = viewModelScope.launch {
        _state.update { it.copy(sharesTab = tab) }
    }

    /**
     * Set the current transfers tab to the UI state
     *
     * @param tab transfer tab to set
     */
    fun setTransfersTab(tab: TransfersTab) = viewModelScope.launch {
        _state.update { it.copy(transfersTab = tab) }
    }

    /**
     * Notify that the node update has been handled by the UI
     */
    fun nodeUpdateHandled() {
        onReceiveNodeUpdate(false)
    }

    /**
     * Set the ui one-off event when a node update is received
     *
     * @param update true if a node update has been received
     */
    private fun onReceiveNodeUpdate(update: Boolean) = viewModelScope.launch {
        _state.update { it.copy(nodeUpdateReceived = update) }
    }

    private val numUnreadUserAlerts = SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>>()

    /**
     * Notifies about the number of unread user alerts once.
     *
     * @return [SingleLiveEvent] with the number of unread user alerts.
     */
    fun onGetNumUnreadUserAlerts(): SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>> =
        numUnreadUserAlerts

    /**
     * Checks the number of unread user alerts.
     */
    fun checkNumUnreadUserAlerts(type: UnreadUserAlertsCheckType) {
        viewModelScope.launch {
            numUnreadUserAlerts.value = Pair(type, getNumUnreadUserAlerts())
        }
    }

    /**
     * Checks the Inbox section visibility.
     */
    fun updateInboxSectionVisibility() {
        viewModelScope.launch {
            _state.update {
                it.copy(hasInboxChildren = hasInboxChildren())
            }
        }
    }

    /**
     * Fire a Media Discovery stats event
     */
    fun onMediaDiscoveryOpened(mediaHandle: Long) {
        viewModelScope.launch {
            sendStatisticsMediaDiscovery(mediaHandle)
        }
    }

    /**
     * Set first login status
     */
    fun setIsFirstLogin(newIsFirstLogin: Boolean) {
        isFirstLogin.update {
            newIsFirstLogin
        }
    }

    /**
     * Set Inbox Node state in ViewModel initially
     */
    fun setInboxNode() {
        viewModelScope.launch {
            inboxNode = getInboxNode()
        }
    }

    /**
     * Get latest [StorageState]
     */
    fun getStorageState() = monitorStorageStateEvent.getState()

    /**
     * Get Cloud Sort Order
     */
    fun getOrder() = runBlocking { getCloudSortOrder() }


    /**
     * After nodes on Cloud Drive changed or some nodes are moved to rubbish bin,
     * need to check CU and MU folders' status.
     *
     * @param shouldDisable If CU or MU folder is deleted by current client, then CU should be disabled. Otherwise not.
     * @param updatedNodes  Nodes which have changed.
     */
    fun checkCameraUploadFolder(shouldDisable: Boolean, updatedNodes: List<Node>?) {
        viewModelScope.launch {
            val primaryHandle = getPrimarySyncHandle()
            val secondaryHandle = getSecondarySyncHandle()
            updatedNodes?.let {
                val nodeMap = it.associateBy { node -> node.id.id }
                // If CU and MU folder don't change then return.
                if (!nodeMap.containsKey(primaryHandle) && !nodeMap.containsKey(secondaryHandle)) {
                    Timber.d("Updated nodes don't include CU/MU, return.")
                    return@launch
                }
            }
            val result = checkCameraUpload(shouldDisable, primaryHandle, secondaryHandle)
            _state.update {
                it.copy(
                    shouldStopCameraUpload = result.shouldStopProcess,
                    shouldSendCameraBroadcastEvent = result.shouldSendEvent,
                )
            }
        }
    }

    /**
     * broadcast upload pause status
     */
    fun broadcastUploadPauseStatus() {
        viewModelScope.launch {
            broadcastUploadPauseState()
        }
    }
}
