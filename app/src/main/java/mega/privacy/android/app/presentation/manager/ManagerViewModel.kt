package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.manager.model.ManagerState
import nz.mega.sdk.*
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to ManagerActivity
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param monitorGlobalUpdates Monitor global updates
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorGlobalUpdates: MonitorGlobalUpdates,
    getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    getBrowserChildrenNode: GetBrowserChildrenNode,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _uiState = MutableStateFlow(initializeState())

    /**
     * public UI State
     */
    val uiState: StateFlow<ManagerState> = _uiState

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
            .mapNotNull { getRubbishBinChildrenNode(_uiState.value.rubbishBinParentHandle) }
            .map { Event(it) }
            .asLiveData()

    /**
     * Update Browser Nodes when a node update callback happens
     */
    val updateBrowserNodes: LiveData<Event<List<MegaNode>>> =
        _updateNodes
            .also { Timber.d("onBrowserNodesUpdate") }
            .mapNotNull { getBrowserChildrenNode(_uiState.value.browserParentHandle) }
            .map { Event(it) }
            .asLiveData()

    /**
     * Set the current browser parent handle to the UI state
     *
     * @param handle the id of the current browser parent handle to set
     */
    fun setBrowserParentHandle(handle: Long) = viewModelScope.launch {
        _uiState.update { it.copy(browserParentHandle = handle) }
    }

    /**
     * Set the current rubbish bin parent handle to the UI state
     *
     * @param handle the id of the current rubbish bin parent handle to set
     */
    fun setRubbishBinParentHandle(handle: Long) = viewModelScope.launch {
        _uiState.update { it.copy(rubbishBinParentHandle = handle) }
    }

    /**
     * Reset the current search drawer item to initial value
     */
    fun resetCurrentSearchDrawerItem() = viewModelScope.launch {
        _uiState.update { it.copy(searchDrawerItem = null) }
    }

    /**
     * Set current search drawer item
     *
     * @param drawerItem
     */
    fun setCurrentSearchDrawerItem(drawerItem: DrawerItem) = viewModelScope.launch {
        _uiState.update { it.copy(searchDrawerItem = drawerItem) }
    }

    /**
     * Set the current search shared tab
     *
     * @param sharedTab
     */
    fun setCurrentSearchSharedTab(sharedTab: Int) = viewModelScope.launch {
        _uiState.update { it.copy(searchSharedTab = sharedTab) }
    }

    /**
     * Set a flag to know if the current navigation level is the first one
     *
     * @param isFirstNavigationLevel true if the current navigation level corresponds to the first level
     */
    fun setIsFirstNavigationLevel(isFirstNavigationLevel: Boolean) = viewModelScope.launch {
        _uiState.update { it.copy(isFirstNavigationLevel = isFirstNavigationLevel) }
    }

    /**
     * Initialize the UI State
     */
    private fun initializeState(): ManagerState =
        ManagerState(
            browserParentHandle = -1L,
            rubbishBinParentHandle = -1L,
            searchDrawerItem = null,
            searchSharedTab = -1,
            isFirstNavigationLevel = true
        )

}