package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.fragments.homepage.Event
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
     * Current rubbish bin parent handle
     */
    var rubbishBinParentHandle: Long = -1L

    /**
     * Current browser parent handle
     */
    var browserParentHandle: Long = -1L

    /**
     * Update Rubbish Nodes when a node update callback happens
     */
    val updateRubbishBinNodes: LiveData<Event<List<MegaNode>>> =
        _updateNodes
            .also { Timber.d("onRubbishNodesUpdate") }
            .mapNotNull { getRubbishBinChildrenNode(rubbishBinParentHandle) }
            .map { Event(it) }
            .asLiveData()


    /**
     * Update Browser Nodes when a node update callback happens
     */
    val updateBrowserNodes: LiveData<Event<List<MegaNode>>> =
        _updateNodes
            .also { Timber.d("onBrowserNodesUpdate") }
            .mapNotNull { getBrowserChildrenNode(browserParentHandle) }
            .map { Event(it) }
            .asLiveData()

    /**
     * Flag to control if a search has been performed
     * It is also used to prevent a search to be performed if the user is not manually triggering it
     */
    var textSubmitted: Boolean = false
}