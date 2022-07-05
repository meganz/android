package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.HasChildren
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to ManagerActivity
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param monitorGlobalUpdates Monitor global updates
 * @param getRubbishBinChildrenNode Fetch the rubbish bin nodes
 * @param getBrowserChildrenNode Fetch the browser nodes
 * @param getRootFolder Fetch the root node
 * @param getRubbishBinChildrenNode
 * @param getBrowserChildrenNode
 * @param getNumUnreadUserAlerts
 * @param getInboxNode
 * @param hasChildren
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorGlobalUpdates: MonitorGlobalUpdates,
    getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    getBrowserChildrenNode: GetBrowserChildrenNode,
    private val getRootFolder: GetRootFolder,
    private val getNumUnreadUserAlerts: GetNumUnreadUserAlerts,
    private val getInboxNode: GetInboxNode,
    private val hasChildren: HasChildren,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ManagerState())

    /**
     * public UI State
     */
    val state: StateFlow<ManagerState> = _state

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
     * Monitor user updates and dispatch to observers
     */
    val updateUsers: LiveData<Event<List<MegaUser>>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .also { Timber.d("onUsersUpdate") }
            .mapNotNull { it.users?.toList() }
            .map { Event(it) }
            .asLiveData()

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

    /**
     * Monitor global node updates and dispatch to observers
     */
    val updateNodes: LiveData<Event<List<MegaNode>>> =
        _updateNodes
            .also { Timber.d("onNodesUpdate") }
            .filterNotNull()
            .map { Event(it) }
            .asLiveData()

    /**
     * Monitor contact request updates and dispatch to observers
     */
    val updateContactsRequests: LiveData<Event<List<MegaContactRequest>>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .also { Timber.d("onContactRequestsUpdate") }
            .mapNotNull { it.requests?.toList() }
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
     * Set the current incoming parent handle to the UI state
     *
     * @param handle the id of the current incoming parent handle to set
     */
    fun setIncomingParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(incomingParentHandle = handle) }
    }

    /**
     * Set the current outgoing parent handle to the UI state
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun setOutgoingParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(outgoingParentHandle = handle) }
    }

    /**
     * Set the current links parent handle to the UI state
     *
     * @param handle the id of the current links parent handle to set
     */
    fun setLinksParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(linksParentHandle = handle) }
    }

    /**
     * Set the current inbox parent handle to the UI state
     *
     * @param handle the id of the current inbox parent handle to set
     */
    fun setInboxParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(inboxParentHandle = handle) }
    }

    /**
     * Set a flag to know if the current navigation level is the first one
     *
     * @param isFirstNavigationLevel true if the current navigation level corresponds to the first level
     */
    fun setIsFirstNavigationLevel(isFirstNavigationLevel: Boolean) = viewModelScope.launch {
        _state.update { it.copy(isFirstNavigationLevel = isFirstNavigationLevel) }
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

    private val inboxSectionVisible: MutableLiveData<Boolean> = MutableLiveData()

    /**
     * Notifies about updates on Inbox section visibility.
     */
    fun onInboxSectionUpdate(): LiveData<Boolean> = inboxSectionVisible

    /**
     * Checks the Inbox section visibility.
     */
    fun checkInboxSectionVisibility() {
        viewModelScope.launch {
            val inboxNode = getInboxNode()
            inboxSectionVisible.value = if (inboxNode == null) false else hasChildren(inboxNode)
        }
    }

}