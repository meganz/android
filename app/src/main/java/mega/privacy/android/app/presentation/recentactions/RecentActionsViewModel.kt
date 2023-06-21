package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsSharesType
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsState
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorHideRecentActivity
import mega.privacy.android.domain.usecase.SetHideRecentActivity
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsFragment]
 *
 * @property getRecentActionsUseCase
 * @property getVisibleContactsUseCase
 * @property setHideRecentActivity
 * @property getNodeByHandle
 * @property getNodeByIdUseCase
 * @property getAccountDetailsUseCase
 * @property areCredentialsVerified
 * @param monitorHideRecentActivity
 * @param monitorNodeUpdates
 */
@HiltViewModel
class RecentActionsViewModel @Inject constructor(
    private val getRecentActionsUseCase: GetRecentActionsUseCase,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val setHideRecentActivity: SetHideRecentActivity,
    private val getNodeByHandle: GetNodeByHandle,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val areCredentialsVerified: AreCredentialsVerified,
    monitorHideRecentActivity: MonitorHideRecentActivity,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    private var _buckets = listOf<RecentActionBucket>()

    /** private UI state */
    private val _state = MutableStateFlow(RecentActionsState())

    /** public UI state */
    val state: StateFlow<RecentActionsState> = _state

    /**
     * Selected recent actions bucket
     */
    var selected: RecentActionBucket? = null

    /**
     * Snapshot of recent actions bucket list when a user select one item
     */
    var snapshotActionList: List<RecentActionBucket>? = null

    init {
        viewModelScope.launch {
            updateRecentActions()
        }

        // monitor node updates
        viewModelScope.launch {
            monitorNodeUpdates()
                .conflate()
                .collect {
                    updateRecentActions()
                }
        }
        // monitor hide recent activity preference
        viewModelScope.launch {
            monitorHideRecentActivity().collectLatest {
                setUiHideRecentActivity(it)
            }
        }
    }

    /**
     * Set the selected recent actions bucket and current recent actions bucket list
     *
     * @param item
     */
    fun select(item: RecentActionItemType.Item) {
        selected = item.bucket
        snapshotActionList = _buckets
    }

    /**
     * Disable hide recent actions activity setting
     */
    fun disableHideRecentActivitySetting() = viewModelScope.launch {
        setHideRecentActivity(false)
    }

    /**
     * Set recent actions items ui state
     *
     * @param list
     */
    private fun setUiRecentActionsItems(list: List<RecentActionItemType>) {
        _state.update { it.copy(recentActionItems = list) }
    }

    /**
     * Set hide recent activity ui state
     */
    private fun setUiHideRecentActivity(hide: Boolean) {
        _state.update { it.copy(hideRecentActivity = hide) }
    }

    /**
     * Update the recent actions list by combination
     */
    private suspend fun updateRecentActions() = coroutineScope {
        val getRecentActions = async {
            getRecentActionsUseCase().also { _buckets = it }
        }
        val getVisibleContacts = async { getVisibleContactsUseCase() }

        val formattedList =
            formatRecentActions(getRecentActions.await(), getVisibleContacts.await())

        setUiRecentActionsItems(formattedList)
    }

    /**
     * Format a list of [RecentActionItemType] from a [RecentActionBucket]
     *
     * @param buckets
     * @return a list of [RecentActionItemType]
     */
    private suspend fun formatRecentActions(
        buckets: List<RecentActionBucket>,
        visibleContacts: List<ContactItem>,
    ): List<RecentActionItemType> {

        val recentItemList = arrayListOf<RecentActionItemType>()
        var previousDate: Long? = null

        buckets.forEach { bucket ->
            // if nodes is null or empty, do not add to the list
            if (bucket.nodes.isEmpty()) return@forEach

            val currentDate = bucket.timestamp

            if (currentDate != previousDate) {
                previousDate = currentDate
                recentItemList.add(RecentActionItemType.Header(currentDate))
            }

            val userName =
                visibleContacts.find { bucket.userEmail == it.email }?.contactData?.fullName.orEmpty()

            val currentUserIsOwner = isCurrentUserOwner(bucket)
            val areCredentialsVerified = runCatching { areCredentialsVerified(bucket.userEmail) }
                .onFailure { Timber.e(it) }
                .getOrDefault(false)
            val isNodeKeyVerified =
                bucket.nodes[0].isNodeKeyDecrypted || areCredentialsVerified
            val parentNode = getNodeByIdUseCase(NodeId(bucket.parentHandle))
            val sharesType = getParentSharesType(parentNode)
            recentItemList.add(
                RecentActionItemType.Item(
                    bucket,
                    userName,
                    parentNode?.name ?: "",
                    sharesType,
                    currentUserIsOwner,
                    isNodeKeyVerified,
                )
            )
        }

        return recentItemList
    }

    private suspend fun isCurrentUserOwner(
        bucket: RecentActionBucket,
    ) = kotlin.runCatching { getAccountDetailsUseCase(false).email == bucket.userEmail }
        .getOrDefault(false)

    /**
     * get a MegaNode by id
     */
    suspend fun getMegaNode(handle: Long) = getNodeByHandle(handle)

    /**
     * Retrieve the parent folder shares type of a node
     *
     * @param node
     */
    private suspend fun getParentSharesType(node: TypedNode?): RecentActionsSharesType {
        return if (node is FolderNode) {
            when {
                node.isIncomingShare -> RecentActionsSharesType.INCOMING_SHARES
                node.isShared -> RecentActionsSharesType.OUTGOING_SHARES
                node.isPendingShare -> RecentActionsSharesType.PENDING_OUTGOING_SHARES
                else -> {
                    val parentNode = getNodeByIdUseCase(node.parentId)
                    getParentSharesType(parentNode)
                }
            }
        } else {
            RecentActionsSharesType.NONE
        }
    }
}
