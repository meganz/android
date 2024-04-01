package mega.privacy.android.app.presentation.recentactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsState
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.recentactions.LegacyGetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionsFragment]
 *
 * @property getRecentActionsUseCase
 * @property getVisibleContactsUseCase
 * @property setHideRecentActivityUseCase
 * @property getNodeByIdUseCase
 * @property getAccountDetailsUseCase
 * @param areCredentialsVerifiedUseCase
 * @param monitorHideRecentActivityUseCase
 * @param monitorNodeUpdatesUseCase
 */
@Deprecated("Should be removed when Compose implementation is released")
@HiltViewModel
class RecentActionsViewModel @Inject constructor(
    private val getRecentActionsUseCase: LegacyGetRecentActionsUseCase,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val setHideRecentActivityUseCase: SetHideRecentActivityUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    monitorHideRecentActivityUseCase: MonitorHideRecentActivityUseCase,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
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
            monitorNodeUpdatesUseCase()
                .catch {
                    Timber.e(it)
                }
                .conflate()
                .collect {
                    updateRecentActions()
                }
        }
        // monitor hide recent activity preference
        viewModelScope.launch {
            monitorHideRecentActivityUseCase().collectLatest {
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
        setHideRecentActivityUseCase(false)
    }

    /**
     * Set recent actions items ui state
     *
     * @param list
     */
    private fun setUiRecentActionsItems(list: List<RecentActionItemType>) {
        _state.update {
            it.copy(
                isLoading = false,
                recentActionItems = list
            )
        }
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
        runCatching {
            val getRecentActions = async {
                getRecentActionsUseCase().also { _buckets = it }
            }
            val getVisibleContacts = async { getVisibleContactsUseCase() }
            val formattedList =
                formatRecentActions(getRecentActions.await(), getVisibleContacts.await())
            setUiRecentActionsItems(formattedList)
        }.onFailure {
            Timber.e(it)
        }
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
        val currentUserEmail = runCatching { getAccountDetailsUseCase(false).email }.getOrNull()

        // For caching the verified credentials
        val verifiedCredentialsMap = mutableMapOf<String, Boolean>()

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

            val currentUserIsOwner = currentUserEmail == bucket.userEmail
            val isNodeKeyVerified = bucket.nodes[0].isNodeKeyDecrypted
                    || currentUserIsOwner
                    || verifiedCredentialsMap.getOrPut(bucket.userEmail) {
                areCredentialsVerified(bucket.userEmail)
            }
            val parentNode = getNodeByIdUseCase(bucket.parentNodeId)
            val sharesType = getParentSharesType(parentNode)
            recentItemList.add(
                RecentActionItemType.Item(
                    bucket,
                    userName,
                    parentNode?.name.orEmpty(),
                    sharesType,
                    currentUserIsOwner,
                    isNodeKeyVerified,
                )
            )
        }

        return recentItemList
    }

    private suspend fun areCredentialsVerified(
        userEmail: String,
    ) = runCatching {
        areCredentialsVerifiedUseCase(userEmail)
    }.getOrDefault(false)

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
