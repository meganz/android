package mega.privacy.android.app.presentation.backups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.app.presentation.backups.model.BackupsState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] class associated to [BackupsFragment]
 *
 * @property getChildrenNode [GetChildrenNode]
 * @property getCloudSortOrder [GetCloudSortOrder]
 * @property getNodeByHandle [GetNodeByHandle]
 * @property getParentNodeHandle [GetParentNodeHandle]
 * @property monitorBackupFolder [MonitorBackupFolder]
 * @property monitorNodeUpdatesUseCase [MonitorNodeUpdatesUseCase]
 * @property monitorViewType [MonitorViewType]
 * @property savedStateHandle [SavedStateHandle]
 */
@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val getChildrenNode: GetChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorViewType: MonitorViewType,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The Backups UI State
     */
    private val _state = MutableStateFlow(BackupsState())

    /**
     * The Backups UI State accessible outside the ViewModel
     */
    val state: StateFlow<BackupsState> = _state

    /**
     * The Backups Folder Node Id from [SavedStateHandle] when Backups was initialized
     */
    private val originalBackupsNodeId =
        NodeId(savedStateHandle.get<Long>(BackupsFragment.PARAM_BACKUPS_HANDLE) ?: -1L)

    /**
     * Performs the following actions upon ViewModel initialization
     */
    init {
        observeNodeUpdates()
        observeUserRootBackupsFolder()
        observeViewType()
        handleOriginalBackupsNodeHandle()
    }

    /**
     * A shorthand way of retrieving the [BackupsState]
     *
     * @return the [BackupsState]
     */
    fun state() = _state.value

    /**
     * Retrieve and update the Child Backups Nodes from the Original Backups Handle received from
     * the [SavedStateHandle]
     */
    private fun handleOriginalBackupsNodeHandle() = viewModelScope.launch {
        runCatching {
            refreshNodes(nodeHandle = originalBackupsNodeId.longValue).also { backupsNodePair ->
                val parentBackupsNode = backupsNodePair.first
                val toolbarName = getToolbarName(parentBackupsNode)

                _state.update {
                    it.copy(
                        originalBackupsNodeId = originalBackupsNodeId,
                        currentBackupsFolderNodeId = originalBackupsNodeId,
                        nodes = backupsNodePair.second,
                        currentBackupsFolderName = toolbarName,
                    )
                }
            }
        }.onFailure {
            Timber.e(it)
            _state.update { state ->
                state.copy(
                    originalBackupsNodeId = originalBackupsNodeId,
                    currentBackupsFolderNodeId = originalBackupsNodeId,
                    nodes = emptyList(),
                    currentBackupsFolderName = null,
                )
            }
        }
    }

    /**
     * Retrieves the Toolbar Title from the Backups node
     *
     * A null Toolbar Title is set if the Backups Node is null or both Backups Node and Root
     * Backups Folder Node handles are the same
     *
     * @param backupsNode A potentially nullable Backups node
     * @return a potentially nullable Toolbar Title
     */
    private fun getToolbarName(backupsNode: MegaNode?): String? = when {
        backupsNode == null ||
                backupsNode.handle == _state.value.rootBackupsFolderNodeId.longValue -> null

        else -> backupsNode.name
    }

    /**
     * Observes any Node Updates through [MonitorNodeUpdatesUseCase]
     */
    private fun observeNodeUpdates() = viewModelScope.launch {
        monitorNodeUpdatesUseCase()
            .catch { Timber.e(it) }
            .collect { _state.update { it.copy(isPendingRefresh = true) } }
    }

    /**
     * Observes the User's Root Backup Folder through [MonitorBackupFolder] and updates
     * [BackupsState.rootBackupsFolderNodeId] when an Update is received
     */
    private fun observeUserRootBackupsFolder() = viewModelScope.launch {
        monitorBackupFolder()
            .catch { Timber.w("Exception monitoring the User's Root Backups Folder: $it") }
            .map {
                Timber.e("Unable to retrieve the User's Root Backups Folder")
                it.getOrDefault(NodeId(MegaApiJava.INVALID_HANDLE))
            }
            .collectLatest { updatedUserRootBackupsFolder ->
                Timber.d("The updated User Root Backups Folder Handle is: ${updatedUserRootBackupsFolder.longValue}")
                _state.update { it.copy(rootBackupsFolderNodeId = updatedUserRootBackupsFolder) }
            }
    }

    /**
     * Uses [monitorViewType] to observe any View Type updates
     */
    private fun observeViewType() = viewModelScope.launch {
        monitorViewType()
            .catch { Timber.e(it) }
            .collect { viewType -> _state.update { it.copy(currentViewType = viewType) } }
    }

    /**
     * Refreshes Backups nodes and hides the selection
     */
    fun refreshBackupsNodesAndHideSelection() = viewModelScope.launch {
        runCatching {
            refreshNodes().let { backupsNodePair ->
                val toolbarName = getToolbarName(backupsNodePair.first)
                _state.update {
                    it.copy(
                        hideMultipleItemSelection = true,
                        nodes = backupsNodePair.second,
                        currentBackupsFolderName = toolbarName,
                    )
                }
            }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Refreshes the list of Backups Nodes
     */
    fun refreshBackupsNodes() = viewModelScope.launch {
        runCatching {
            refreshNodes().also { backupsNodePair ->
                val toolbarName = getToolbarName(backupsNodePair.first)
                _state.update {
                    it.copy(
                        nodes = backupsNodePair.second,
                        currentBackupsFolderName = toolbarName,
                    )
                }
            }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Retrieves the list of Nodes
     *
     * @param nodeHandle The Node Handle used to retrieve the current list of Nodes. Defaults to [BackupsState.currentBackupsFolderNodeId]
     *
     * @return a Pair containing the potentially nullable Parent Backups Node and its Children Backups Nodes
     */
    private suspend fun refreshNodes(nodeHandle: Long = _state.value.currentBackupsFolderNodeId.longValue): Pair<MegaNode?, List<MegaNode>> {
        val parentBackupsNode = getNodeByHandle(nodeHandle)
        val childrenBackupsNodes = parentBackupsNode?.let {
            getChildrenNode(
                parent = parentBackupsNode,
                order = getCloudSortOrder()
            )
        } ?: emptyList()
        return Pair(parentBackupsNode, childrenBackupsNodes)
    }

    /**
     * Handles the Back Press Behavior
     *
     * Either one of the following conditions will cause the User to exit the Backups page:
     *
     * 1. The User is in the same page as with the Original Backups Node Handle
     * 2. The Parent Backup Node Handle of [BackupsState.currentBackupsFolderNodeId] is null
     */
    fun handleBackPress() {
        viewModelScope.launch {
            val currentBackupsFolderNodeId = _state.value.currentBackupsFolderNodeId
            if (currentBackupsFolderNodeId == originalBackupsNodeId) {
                onExitBackups(true)
            } else {
                runCatching {
                    val parentBackupsNodeHandle =
                        getParentNodeHandle(currentBackupsFolderNodeId.longValue)
                    parentBackupsNodeHandle?.let { nonNullParentBackupsNodeHandle ->
                        refreshNodes(nodeHandle = nonNullParentBackupsNodeHandle).also { backupsNodePair ->
                            val toolbarName = getToolbarName(backupsNodePair.first)
                            _state.update {
                                it.copy(
                                    currentBackupsFolderNodeId = NodeId(
                                        nonNullParentBackupsNodeHandle
                                    ),
                                    triggerBackPress = true,
                                    nodes = backupsNodePair.second,
                                    currentBackupsFolderName = toolbarName,
                                )
                            }
                        }
                    } ?: onExitBackups(true)
                }.onFailure { Timber.e(it) }
            }
        }
    }

    /**
     * Notifies [BackupsState.shouldExitBackups] that the Backups screen has acknowledged to exit the screen
     */
    fun exitBackupsHandled() {
        onExitBackups(false)
    }

    /**
     * Updates the value of [BackupsState.shouldExitBackups]
     *
     * @param exitBackups Whether the User should exit the Backups page or not
     */
    private fun onExitBackups(exitBackups: Boolean) {
        _state.update { it.copy(shouldExitBackups = exitBackups) }
    }

    /**
     * Notifies [BackupsState.triggerBackPress] that the Backups screen has handled the Back Press
     * behavior by setting its value to false
     */
    fun triggerBackPressHandled() {
        _state.update { it.copy(triggerBackPress = false) }
    }

    /**
     * Updates the Current Backups Folder Node ID [BackupsState.currentBackupsFolderNodeId]
     *
     * @param nodeHandle The new Backups Handle
     */
    fun updateBackupsHandle(nodeHandle: Long) {
        _state.update { it.copy(currentBackupsFolderNodeId = NodeId(nodeHandle)) }
    }

    /**
     * Returns the [Long] equivalent of Current Backups Folder Node ID [BackupsState.currentBackupsFolderNodeId]
     *
     * @return The Current Backups Folder Handle
     */
    fun getCurrentBackupsFolderHandle() = _state.value.currentBackupsFolderNodeId.longValue

    /**
     * Checks whether the User is currently in the Root Backups Folder level or not
     *
     * @return true if the User is currently in the Root Backups Folder level, and false if otherwise
     */
    fun isUserInRootBackupsFolderLevel() = _state.value.isUserInRootBackupsFolderLevel

    /**
     * Returns the Toolbar Name from [BackupsState.currentBackupsFolderName]
     *
     * @return The Toolbar Name
     */
    fun getToolbarName() = _state.value.currentBackupsFolderName

    /**
     * Notifies [BackupsState.hideMultipleItemSelection] that the Backups screen has handled the hiding
     * of the Multiple Item Selection by setting its value to false
     */
    fun hideMultipleItemSelectionHandled() {
        _state.update { it.copy(hideMultipleItemSelection = false) }
    }

    /**
     * Get Cloud Sort Order
     */
    fun getOrder() = runBlocking { getCloudSortOrder() }

    /**
     * Mark handled pending refresh
     */
    fun markHandledPendingRefresh() {
        _state.update { it.copy(isPendingRefresh = false) }
    }
}
