package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import nz.mega.sdk.*
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [mega.privacy.android.app.main.ManagerActivity]
 *
 * @param monitorNodeUpdates Monitor global node updates
 * @param monitorGlobalUpdates Monitor global updates
 * @param getRubbishBinChildrenNode
 * @param getBrowserChildrenNode
 * @param getNumUnreadUserAlerts
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorGlobalUpdates: MonitorGlobalUpdates,
    getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    getBrowserChildrenNode: GetBrowserChildrenNode,
    private val getNumUnreadUserAlerts: GetNumUnreadUserAlerts
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
}