package mega.privacy.android.app.presentation.recentactions.recentactionbucket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUIState
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetRootParentNodeUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.UpdateRecentAction
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentActionBucketFragment]
 */
@HiltViewModel
class RecentActionBucketViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val updateRecentAction: UpdateRecentAction,
    private val getRecentActionNodes: GetRecentActionNodes,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val getRootParentNodeUseCase: GetRootParentNodeUseCase,
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {
    private val _actionMode = MutableLiveData<Boolean>()

    /**
     * True if the actionMode should to be visible
     */
    val actionMode: LiveData<Boolean> = _actionMode

    private val _nodesToAnimate = MutableLiveData<Set<Int>>()

    /**
     * Set of node positions to animate
     */
    val nodesToAnimate: LiveData<Set<Int>> = _nodesToAnimate

    private val selectedNodes: MutableSet<NodeItem> = mutableSetOf()

    /**
     * Current bucket
     */
    private val _bucket: MutableStateFlow<RecentActionBucket?> = MutableStateFlow(null)
    val bucket = _bucket.asStateFlow()

    private var cachedActionList: List<RecentActionBucket>? = null

    private val _shouldCloseFragment: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * True if the fragment needs to be closed
     */
    val shouldCloseFragment: LiveData<Boolean> = _shouldCloseFragment

    /**
     * True if the parent of the bucket is an incoming shares
     */
    var isInShare = false

    //This flag is used to determine whether the uiDirty state needs to be cleared.
    private var isClearUiDirty = false

    /**
     *  List of node items in the current bucket
     */
    val items = _bucket
        .map { it?.let { getRecentActionNodes(it.nodes) } ?: emptyList() }
        .onEach {
            isInShare = it.firstOrNull()?.node?.let { node ->
                getRootParentNodeUseCase(NodeId(node.handle))?.isIncomingShare
            } ?: false
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _state = MutableStateFlow(RecentActionBucketUIState())

    val state = _state.asStateFlow()

    @Volatile
    private var showHiddenItems: Boolean = true

    init {
        viewModelScope.launch {
            monitorNodeUpdatesUseCase().collectLatest {
                Timber.d("Received node update")
                updateCurrentBucket(excludeSensitives = !showHiddenItems)
                clearSelection()
            }
        }

        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .collect { accountDetail ->
                    val accountType = accountDetail.levelDetail?.accountType
                    val businessStatus =
                        if (accountType?.isBusinessAccount == true) {
                            getBusinessStatusUseCase()
                        } else null

                    val isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired
                    _state.update {
                        it.copy(
                            accountType = accountDetail.levelDetail?.accountType,
                            isBusinessAccountExpired = isBusinessAccountExpired,
                        )
                    }
                }
        }

        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }

        monitorShowHiddenItems()
    }

    /**
     * Check if the current bucket is set
     *
     * @return true if the current bucket is set
     */
    fun isCurrentBucketSet(): Boolean = _bucket.value != null

    /**
     * Set bucket value
     *
     * @param selectedBucket
     */
    fun setBucket(selectedBucket: RecentActionBucket?) = viewModelScope.launch {
        _bucket.emit(selectedBucket)
    }

    /**
     * Set cached action list
     *
     * @param cachedActions
     */
    fun setCachedActionList(cachedActions: List<RecentActionBucket>?) {
        cachedActionList = cachedActions
    }

    /**
     * Get the selected nodes
     *
     * @return the selected nodes
     */
    fun getSelectedNodes(): List<NodeItem> = selectedNodes.toList()

    /**
     * Retrieves the list of non-null [MegaNode] objects from [selectedNodes]
     *
     * @return A list of nun-null [MegaNode] objects
     */
    fun getSelectedMegaNodes(): List<MegaNode> =
        selectedNodes.toList().mapNotNull { it.node }

    /**
     * Checks whether any [MegaNode] in [getSelectedMegaNodes] belongs in Backups
     *
     * @return True if at least one [MegaNode] belongs in Backups, and False if otherwise
     */
    fun isAnyNodeInBackups(): Boolean =
        getSelectedMegaNodes().any { node -> megaApi.isInVault(node) }

    /**
     * Get the count of selected nodes
     *
     * @return the count of selected nodes
     */
    fun getSelectedNodesCount(): Int = selectedNodes.size

    /**
     * Get the count of nodes
     *
     * @return the count of nodes
     */
    fun getNodesCount(): Int = items.value.size

    /**
     * Clear selected nodes
     */
    fun clearSelection() {
        _actionMode.value = false
        selectedNodes.clear()

        val animNodeIndices = mutableSetOf<Int>()
        val nodeList = items.value

        for ((position, node) in nodeList.withIndex()) {
            if (node in selectedNodes) {
                animNodeIndices.add(position)
            }
            node.selected = false
            node.uiDirty = true
        }

        _nodesToAnimate.value = animNodeIndices
    }

    /**
     * Receive on node long click
     *
     * @param position the position of the item in the adapter
     * @param node the node item
     */
    fun onNodeLongClicked(position: Int, node: NodeItem) {
        if (isClearUiDirty) {
            if (selectedNodes.isEmpty()) {
                items.value.forEach { it.uiDirty = true }
            }
            isClearUiDirty = false
        }

        val nodeList = items.value

        if (position < 0 || position >= nodeList.size || nodeList[position].hashCode() != node.hashCode()
        ) {
            return
        }

        nodeList[position].selected = !nodeList[position].selected

        if (nodeList[position] !in selectedNodes) {
            selectedNodes.add(node)
        } else {
            selectedNodes.remove(node)
        }

        nodeList[position].uiDirty = true
        _actionMode.value = selectedNodes.isNotEmpty()

        _nodesToAnimate.value = hashSetOf(position)
    }

    /**
     * Select all nodes
     */
    fun selectAll() {
        val nodeList = items.value

        val animNodeIndices = mutableSetOf<Int>()

        for ((position, node) in nodeList.withIndex()) {
            if (!node.selected) {
                animNodeIndices.add(position)
            }
            node.selected = true
            node.uiDirty = true
            selectedNodes.add(node)
        }

        _nodesToAnimate.value = animNodeIndices
        _actionMode.value = true
    }

    /**
     * Update the current bucket
     */
    private suspend fun updateCurrentBucket(excludeSensitives: Boolean) {
        _bucket.value
            ?.let { updateRecentAction(it, cachedActionList, excludeSensitives) }
            ?.let { _bucket.emit(it) }
            ?: run {
                // No nodes contained in the bucket or the action bucket is no loner exists.
                _shouldCloseFragment.postValue(true)
            }
    }

    fun hideOrUnhideNodes(nodeIds: List<NodeId>, hide: Boolean) = viewModelScope.launch {
        isClearUiDirty = true
        nodeIds.forEach {
            async {
                runCatching {
                    updateNodeSensitiveUseCase(nodeId = it, isSensitive = hide)
                }.onFailure { throwable -> Timber.e("Update sensitivity failed: $throwable") }
            }
        }
    }

    /**
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    private fun monitorShowHiddenItems() {
        monitorShowHiddenItemsUseCase()
            .conflate()
            .onEach { show ->
                showHiddenItems = show
                updateCurrentBucket(excludeSensitives = !show)
            }
            .launchIn(viewModelScope)
    }

    internal suspend fun getNodeContentUri(handle: Long) = getNodeContentUriByHandleUseCase(handle)
}
