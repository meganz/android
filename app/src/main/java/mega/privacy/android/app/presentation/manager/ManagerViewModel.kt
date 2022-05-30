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
    private val updates = monitorGlobalUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor user updates and dispatch to observers
     */
    val updateUsers: LiveData<List<MegaUser>> =
        updates
            .filterIsInstance<GlobalUpdate.OnUsersUpdate>()
            .also { Timber.d("onUsersUpdate") }
            .map { it.users?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()

    /**
     * Monitor user alerts updates and dispatch to observers
     */
    val updateUserAlerts: LiveData<List<MegaUserAlert>> =
        updates
            .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
            .also { Timber.d("onUserAlertsUpdate") }
            .map { it.userAlerts?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()

    /**
     * Monitor global node updates and dispatch to observers
     */
    val updateNodes: LiveData<List<MegaNode>> =
        monitorNodeUpdates()
            .also { Timber.d("onNodesUpdate") }
            .filterNot { it.isEmpty() }
            .asLiveData()

    /**
     * Monitor contact request updates and dispatch to observers
     */
    val updateContactsRequests: LiveData<List<MegaContactRequest>> =
        updates
            .filterIsInstance<GlobalUpdate.OnContactRequestsUpdate>()
            .also { Timber.d("onContactRequestsUpdate") }
            .map { it.requests?.toList() }
            .filterNotNull()
            .filterNot { it.isEmpty() }
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
        updateNodes.asFlow()
            .also { Timber.d("onRubbishNodesUpdate") }
            .map { getRubbishBinChildrenNode(rubbishBinParentHandle) }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()


    /**
     * Update Browser Nodes when a node update callback happens
     */
    val updateBrowserNodes: LiveData<List<MegaNode>> =
        updateNodes.asFlow()
            .also { Timber.d("onBrowserNodesUpdate") }
            .map { getBrowserChildrenNode(browserParentHandle) }
            .filterNotNull()
            .filterNot { it.isEmpty() }
            .asLiveData()
}