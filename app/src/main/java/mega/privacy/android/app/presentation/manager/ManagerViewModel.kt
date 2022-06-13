package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.*
import nz.mega.sdk.*
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [mega.privacy.android.app.main.ManagerActivity]
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * Monitor global node updates
     */
    private val _updateNodes = monitorNodeUpdates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * Monitor user updates and dispatch to observers
     */
    val updateUsers: LiveData<List<MegaUser>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .also { Timber.d("onUsersUpdate") }
            .map { it.users?.toList() }
            .filterNotNull()
            .asLiveData()

    /**
     * Monitor user alerts updates and dispatch to observers
     */
    val updateUserAlerts: LiveData<List<MegaUserAlert>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
            .also { Timber.d("onUserAlertsUpdate") }
            .map { it.userAlerts?.toList() }
            .filterNotNull()
            .asLiveData()

    /**
     * Monitor global node updates and dispatch to observers
     */
    val updateNodes: LiveData<List<MegaNode>> =
        _updateNodes
            .also { Timber.d("onNodesUpdate") }
            .filterNotNull()
            .asLiveData()

    /**
     * Monitor contact request updates and dispatch to observers
     */
    val updateContactsRequests: LiveData<List<MegaContactRequest>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .also { Timber.d("onContactRequestsUpdate") }
            .map { it.requests?.toList() }
            .filterNotNull()
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
    val updateRubbishBinNodes: LiveData<List<MegaNode>> =
        _updateNodes
            .also { Timber.d("onRubbishNodesUpdate") }
            .map { getRubbishBinChildrenNode(rubbishBinParentHandle) }
            .filterNotNull()
            .asLiveData()


    /**
     * Update Browser Nodes when a node update callback happens
     */
    val updateBrowserNodes: LiveData<List<MegaNode>> =
        _updateNodes
            .also { Timber.d("onBrowserNodesUpdate") }
            .map { getBrowserChildrenNode(browserParentHandle) }
            .filterNotNull()
            .asLiveData()
}