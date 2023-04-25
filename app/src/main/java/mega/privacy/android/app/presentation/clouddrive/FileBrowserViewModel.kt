package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetFileBrowserChildrenUseCase
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import nz.mega.sdk.MegaApiJava
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to FileBrowserFragment
 *
 * @param getRootFolder Fetch the root node
 * @param getBrowserChildrenNode Fetch the cloud drive nodes
 * @param monitorMediaDiscoveryView Monitor media discovery view settings
 * @param monitorNodeUpdates Monitor node updates
 * @param getFileBrowserParentNodeHandle To get parent handle of current node
 * @param getIsNodeInRubbish To get current node is in rubbish
 * @param getFileBrowserChildrenUseCase [GetFileBrowserChildrenUseCase]
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val getRootFolder: GetRootFolder,
    private val getBrowserChildrenNode: GetBrowserChildrenNode,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val getFileBrowserParentNodeHandle: GetParentNodeHandle,
    private val getIsNodeInRubbish: IsNodeInRubbish,
    private val getFileBrowserChildrenUseCase: GetFileBrowserChildrenUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FileBrowserState())

    /**
     * State flow
     */
    val state: StateFlow<FileBrowserState> = _state

    /**
     * Stack to maintain folder navigation clicks
     */
    private val lastPositionStack = Stack<Int>()
    private val handleStack = Stack<Long>()

    init {
        monitorMediaDiscovery()
        refreshNodes()
        monitorFileBrowserChildrenNodes()
    }

    /**
     * This will monitor media discovery from [MonitorMediaDiscoveryView] and update
     * [FileBrowserState.mediaDiscoveryViewSettings]
     */
    private fun monitorMediaDiscovery() {
        viewModelScope.launch {
            monitorMediaDiscoveryView().collect { mediaDiscoveryViewSettings ->
                _state.update {
                    it.copy(
                        mediaDiscoveryViewSettings = mediaDiscoveryViewSettings
                            ?: MediaDiscoveryViewSettings.INITIAL.ordinal
                    )
                }
            }
        }
    }

    /**
     * This will monitor FileBrowserNodeUpdates from [MonitorNodeUpdates] and
     * will update [FileBrowserState.nodes]
     */
    private fun monitorFileBrowserChildrenNodes() {
        viewModelScope.launch {
            monitorNodeUpdates().collect {
                checkForNodeIsInRubbish(it.changes)
            }
        }
    }

    /**
     * This will update handle for fileBrowser if any node is deleted from browser and
     * moved to rubbish bin
     * we are in same screen else will simply refresh nodes with parentID
     * @param changes [Map] of [Node], list of [NodeChanges]
     */
    private suspend fun checkForNodeIsInRubbish(changes: Map<Node, List<NodeChanges>>) {
        changes.forEach { (node, _) ->
            if (node is FolderNode) {
                if (node.isInRubbishBin && _state.value.fileBrowserHandle == node.id.longValue) {
                    while (handleStack.isNotEmpty() && getIsNodeInRubbish(handleStack.peek())) {
                        handleStack.pop()
                    }
                    handleStack.takeIf { stack -> stack.isNotEmpty() }?.peek()?.let { parent ->
                        setBrowserParentHandle(parent)
                        return
                    }
                }
            }
        }
        setPendingRefreshNodes()
    }

    private fun setPendingRefreshNodes() {
        _state.update { it.copy(isPendingRefresh = true) }
    }

    /**
     * Set the current browser handle to the UI state
     *
     * @param handle the id of the current browser handle to set
     */
    fun setBrowserParentHandle(handle: Long) = viewModelScope.launch {
        handleStack.push(handle)
        _state.update {
            it.copy(
                fileBrowserHandle = handle,
                mediaHandle = handle
            )
        }
        refreshNodes()
    }

    /**
     * Get the browser parent handle
     * If not previously set, set the browser parent handle to root handle
     *
     * @return the handle of the browser section
     */
    fun getSafeBrowserParentHandle(): Long = runBlocking {
        if (_state.value.fileBrowserHandle == -1L) {
            setBrowserParentHandle(getRootFolder()?.handle ?: MegaApiJava.INVALID_HANDLE)
        }
        return@runBlocking _state.value.fileBrowserHandle
    }

    /**
     * If a folder only contains images or videos, then go to MD mode directly
     *
     * @param parentHandle the folder handle
     * @param mediaDiscoveryViewSettings [mediaDiscoveryViewSettings]
     * @return true is should enter MD mode, otherwise is false
     */
    suspend fun shouldEnterMediaDiscoveryMode(
        parentHandle: Long,
        mediaDiscoveryViewSettings: Int,
    ): Boolean =
        getBrowserChildrenNode(parentHandle)?.let { nodes ->
            if (nodes.isEmpty() || mediaDiscoveryViewSettings == MediaDiscoveryViewSettings.DISABLED.ordinal) {
                false
            } else {
                nodes.firstOrNull { node ->
                    node.isFolder
                            || MimeTypeList.typeForName(node.name).isSvgMimeType
                            || (!MimeTypeList.typeForName(node.name).isImage
                            && !MimeTypeList.typeForName(node.name).isVideoMimeType)
                }?.let {
                    false
                } ?: true
            }
        } ?: false

    /**
     * This will refresh file browser nodes and update [FileBrowserState.nodes]
     */
    fun refreshNodes() {
        viewModelScope.launch {
            val nodeList =
                getNodeUiItems(getFileBrowserChildrenUseCase(_state.value.fileBrowserHandle))
            _state.update {
                it.copy(
                    nodes = getBrowserChildrenNode(_state.value.fileBrowserHandle) ?: emptyList(),
                    parentHandle = getFileBrowserParentNodeHandle(_state.value.fileBrowserHandle),
                    nodesList = nodeList
                )
            }
        }
    }

    /**
     * This will map list of [Node] to [NodeUIItem]
     */
    private fun getNodeUiItems(nodeList: List<Node>): List<NodeUIItem> {
        val existingNodeList = _state.value.nodesList
        return nodeList.mapIndexed { index, it ->
            NodeUIItem(
                node = it,
                isSelected = if (existingNodeList.size > index) existingNodeList[index].isSelected else false,
                isInvisible = if (existingNodeList.size > index) existingNodeList[index].isInvisible else false
            )
        }
    }

    /**
     * Handles back click of rubbishBinFragment
     */
    fun onBackPressed() {
        _state.value.parentHandle?.let {
            setBrowserParentHandle(it)
            handleStack.takeIf { stack -> stack.isNotEmpty() }?.pop()
        }
    }

    /**
     * Pop scroll position for previous depth
     *
     * @return last position saved
     */
    fun popLastPositionStack(): Int = lastPositionStack.takeIf { it.isNotEmpty() }?.pop() ?: 0

    /**
     * Push lastPosition to stack
     * @param lastPosition last position to be added to stack
     */
    private fun pushPositionOnStack(lastPosition: Int) {
        lastPositionStack.push(lastPosition)
    }

    /**
     * Performs action when folder is clicked from adapter
     * @param lastFirstVisiblePosition visible position based on listview type
     * @param handle node handle
     */
    fun onFolderItemClicked(lastFirstVisiblePosition: Int, handle: Long) {
        pushPositionOnStack(lastFirstVisiblePosition)
        setBrowserParentHandle(handle)
    }

    /**
     * Updates the value of [FileBrowserState.currentViewType]
     *
     * @param newViewType The new [ViewType]
     */
    fun setCurrentViewType(newViewType: ViewType) {
        _state.update { it.copy(currentViewType = newViewType) }
    }

    /**
     * Mark handled pending refresh
     *
     */
    fun markHandledPendingRefresh() {
        _state.update { it.copy(isPendingRefresh = false) }
    }
}