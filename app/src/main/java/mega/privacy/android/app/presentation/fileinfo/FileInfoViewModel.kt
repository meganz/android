package mega.privacy.android.app.presentation.fileinfo

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.offline.SetNodeAvailableOffline
import mega.privacy.android.app.domain.usecase.shares.GetOutShares
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges.Inshare
import mega.privacy.android.domain.entity.node.NodeChanges.Name
import mega.privacy.android.domain.entity.node.NodeChanges.Outshare
import mega.privacy.android.domain.entity.node.NodeChanges.Owner
import mega.privacy.android.domain.entity.node.NodeChanges.Parent
import mega.privacy.android.domain.entity.node.NodeChanges.Public_link
import mega.privacy.android.domain.entity.node.NodeChanges.Remove
import mega.privacy.android.domain.entity.node.NodeChanges.Timestamp
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeById
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.IsAvailableOffline
import mega.privacy.android.domain.usecase.IsNodeInInbox
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MonitorChildrenUpdates
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.filenode.CopyNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandle
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions
import mega.privacy.android.domain.usecase.filenode.GetNodeVersionsByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeByHandle
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.StopSharingNode
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * View Model class for [FileInfoActivity]
 */
@HiltViewModel
class FileInfoViewModel @Inject constructor(
    private val tempMegaNodeRepository: MegaNodeRepository, //a temp use of MegaApiAndroid, just while migrating to use-cases only, to easily remove things from the activity
    private val fileUtilWrapper: FileUtilWrapper,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorConnectivity: MonitorConnectivity,
    private val getFileHistoryNumVersions: GetFileHistoryNumVersions,
    private val isNodeInInbox: IsNodeInInbox,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val checkNameCollision: CheckNameCollision,
    private val moveNodeByHandle: MoveNodeByHandle,
    private val copyNodeByHandle: CopyNodeByHandle,
    private val moveNodeToRubbishByHandle: MoveNodeToRubbishByHandle,
    private val deleteNodeByHandle: DeleteNodeByHandle,
    private val deleteNodeVersionsByHandle: DeleteNodeVersionsByHandle,
    private val getPreview: GetPreview,
    private val getNodeById: GetNodeById,
    private val getFolderTreeInfo: GetFolderTreeInfo,
    private val getContactItemFromInShareFolder: GetContactItemFromInShareFolder,
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById,
    private val monitorChildrenUpdates: MonitorChildrenUpdates,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val getNodeVersionsByHandle: GetNodeVersionsByHandle,
    private val getOutShares: GetOutShares,
    private val getNodeLocationInfo: GetNodeLocationInfo,
    private val isAvailableOffline: IsAvailableOffline,
    private val setNodeAvailableOffline: SetNodeAvailableOffline,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val stopSharingNode: StopSharingNode,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileInfoViewState())

    /**
     * the state of the view
     */
    val uiState = _uiState.asStateFlow()

    /**
     * the node whose information are displayed
     */
    lateinit var node: MegaNode //this should be removed as all uses should be replaced by typedNode
        private set

    /**
     * the node whose information are displayed
     */
    lateinit var typedNode: TypedNode
        private set

    private var versions: List<Node>? = null
    private val monitoringJobs = ArrayList<Job?>()
    private val monitoringMutex = Mutex()

    /**
     * This initial setup will be removed once MegaNode is not needed anymore in future tasks
     */
    @Deprecated(
        message = "This initial setup will be removed once MegaNode is not needed",
        replaceWith = ReplaceWith("setNode(handleNode: Long)")
    )
    fun tempInit(node: MegaNode) {
        this.node = node
        setNode(node.handle)
    }

    /**
     * Sets the node and updates its state
     */
    fun setNode(handleNode: Long) {
        Timber.d("FileInfoViewModel node set $handleNode")
        viewModelScope.launch {
            runCatching {
                typedNode = getNodeById(NodeId(handleNode))
                node = tempMegaNodeRepository.getNodeByHandle(handleNode)
                    ?: throw RuntimeException()
            }.onFailure {
                Timber.e("FileInfoViewModel node not found")
                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NodeDeleted)
                return@launch
            }

            if (typedNode is FileNode) {
                val parent = getNodeById(typedNode.parentId)
                if (parent is FileNode) {
                    //we only want the latest version in this screen
                    setNode(parent.id.longValue)
                    return@launch
                }
                versions = getNodeVersionsByHandle(typedNode.id)
            }
            updateCurrentNodeStatus()
            //monitor future changes of the node
            monitoringMutex.withLock {
                monitoringJobs.forEach { it?.cancel() }
                monitoringJobs.clear()
                monitoringJobs.addAll(
                    listOf(
                        monitorNodeUpdates(),
                        monitorChildrenUpdates(),
                        monitorSharesContactUpdates(),
                    )
                )
            }
        }
    }

    /**
     * Check if the device is connected to Internet and fire and event if not
     */
    fun checkAndHandleIsDeviceConnected() =
        if (!isConnected) {
            _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NotConnected)
            false
        } else {
            true
        }

    /**
     * Tries to move the node to the destination parentHandle node
     * First it checks for collisions, firing the [FileInfoOneOffViewEvent.CollisionDetected] event if corresponds
     * It sets [FileInfoJobInProgressState.Moving] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    fun moveNodeCheckingCollisions(parentHandle: NodeId) =
        performBlockSettingProgress(FileInfoJobInProgressState.Moving) {
            if (checkCollision(parentHandle, NameCollisionType.MOVE)) {
                runCatching {
                    moveNodeByHandle(typedNode.id, parentHandle)
                }
            } else {
                null
            }
        }

    /**
     * Tries to copy the node to the destination parentHandle node
     * First it checks for collisions, firing the [FileInfoOneOffViewEvent.CollisionDetected] event if corresponds
     * It sets [FileInfoJobInProgressState.Copying] while copying.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    fun copyNodeCheckingCollisions(parentHandle: NodeId) =
        performBlockSettingProgress(FileInfoJobInProgressState.Copying) {
            if (checkCollision(parentHandle, NameCollisionType.COPY)) {
                runCatching {
                    copyNodeByHandle(typedNode.id, parentHandle)
                }
            } else {
                null
            }
        }

    /**
     * It checks if the node is in the rubbish bin,
     * if it's not in the rubbish bin, moves the node to the rubbish bin
     * if it's already in the rubbish bin, deletes the node.
     * It will sets the proper [FileInfoJobInProgressState] and launch the proper [FileInfoOneOffViewEvent.Finished]
     */
    fun removeNode() {
        if (_uiState.value.isNodeInRubbish) {
            deleteNode()
        } else {
            moveNodeToRubbishBin()
        }
    }

    /**
     * Deletes the history versions of the node
     * It sets [FileInfoJobInProgressState.DeletingVersions] while deleting.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    fun deleteHistoryVersions() {
        performBlockSettingProgress(FileInfoJobInProgressState.DeletingVersions) {
            runCatching {
                deleteNodeVersionsByHandle(typedNode.id)
            }
        }
    }


    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent(event: FileInfoOneOffViewEvent) {
        if (_uiState.value.oneOffViewEvent == event) {
            _uiState.updateEventAndClearProgress(null)
        }
    }

    /**
     * change the state of isShareContactExpanded
     */
    fun expandOutSharesClick() {
        updateState { it.copy(isShareContactExpanded = !it.isShareContactExpanded) }
    }

    /**
     * Change the current offline availability
     */
    fun availableOfflineChanged(
        availableOffline: Boolean,
        activity: WeakReference<Activity>,
    ) {
        if (availableOffline == _uiState.value.isAvailableOffline) return
        if (availableOffline && monitorStorageStateEvent.getState() == StorageState.PayWall) {
            updateState { it.copy(oneOffViewEvent = FileInfoOneOffViewEvent.OverDiskQuota) }
            return
        }
        updateState {
            it.copy(isAvailableOfflineEnabled = false) // to avoid multiple changes while changing
        }
        viewModelScope.launch {
            setNodeAvailableOffline(
                typedNode.id,
                availableOffline,
                activity
            )
            updateState {
                it.copy(
                    oneOffViewEvent = if (!availableOffline) FileInfoOneOffViewEvent.Message.RemovedOffline else null,
                    isAvailableOffline = availableOffline,
                    isAvailableOfflineEnabled = !typedNode.isTakenDown && !it.isNodeInRubbish,
                )
            }
        }
    }

    /**
     * Stop sharing this node
     */
    fun stopSharing() {
        updateState {
            stopSharingNode(typedNode.id)
            it.copy(typedNode = getNodeById(typedNode.id))
        }
    }

    private fun monitorNodeUpdates() =
        viewModelScope.launch {
            monitorNodeUpdatesById(typedNode.id).collect { changes ->
                //node has been updated
                Timber.d("FileInfoViewModel updated $changes")
                val updateNode = changes.any { change ->
                    when (change) {
                        Remove -> {
                            versions?.getOrNull(1)?.let { version ->
                                //check the newest version if exists and set it as current
                                setNode(version.id.longValue)
                            } ?: run {
                                //if there are no versions, just finish
                                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NodeDeleted)
                            }
                        }
                        Name -> updateTitle()
                        Owner -> updateOwner()
                        Inshare -> updateAccessPermission()
                        Parent ->
                            if (!_uiState.value.isNodeInRubbish && isNodeInRubbish(typedNode.id.longValue)) {
                                //if the node is moved to rubbish bin, let's close the screen like when it's deleted
                                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NodeDeleted)
                            } else {
                                // maybe it's moved, or a new version is created, etc. let's update all to be safe
                                return@any true
                            }
                        Timestamp -> return@any true //will update only dates, for now update everything
                        Outshare -> updateOutShares()
                        Public_link -> return@any true //will update only public link, for now update everything
                        else -> return@any false
                    }
                    return@any false
                }
                if (updateNode) {
                    setNode(typedNode.id.longValue)
                }
            }
        }

    private fun monitorChildrenUpdates() =
        viewModelScope.launch {
            monitorChildrenUpdates(typedNode.id).collect {
                //folder node content or file versions have been updated
                updateFolderTreeInfo()
                updateHistory()
            }
        }

    private fun monitorSharesContactUpdates() =
        viewModelScope.launch {
            monitorContactUpdates().collect { userUpdate ->
                Timber.d("FileInfoViewModel monitorOutSharesContactUpdates ${userUpdate.changes}")
                val relevantChanges = listOf(
                    UserChanges.Alias,
                    UserChanges.Email,
                    UserChanges.Firstname,
                    UserChanges.Lastname,
                    UserChanges.LastInteractionTimestamp
                )
                //only monitor relevant changes for this screen
                if (userUpdate.changes.values.flatten().any(relevantChanges::contains)) {
                    if (_uiState.value.outSharesCoerceMax.isEmpty().not()) {
                        //a possible change in outShares contacts, let's update
                        updateOutShares()
                    }
                    _uiState.value.inShareOwnerContactItem?.handle?.let { userId ->
                        if (userUpdate.changes.keys.any { it.id == userId }) {
                            // update in owner contact, let's update
                            updateOwner()
                        }
                    }
                }
            }
        }

    private fun updateCurrentNodeStatus() {
        updateState { uiState ->
            val inShareOwnerContactItem = (typedNode as? TypedFolderNode)?.let {
                //a first cached fast version, later we'll ask for a fresh ContactItem
                getContactItemFromInShareFolder(folderNode = it, skipCache = false)
            }
            val isNodeInRubbish = isNodeInRubbish(typedNode.id.longValue)
            uiState.copy(
                typedNode = typedNode,
                isNodeInInbox = isNodeInInbox(typedNode.id.longValue),
                isNodeInRubbish = isNodeInRubbish,
                jobInProgressState = null,
                isAvailableOffline = isAvailableOffline(typedNode),
                isAvailableOfflineEnabled = !typedNode.isTakenDown && !isNodeInRubbish,
                thumbnailUriString = (typedNode as? TypedFileNode)?.thumbnailPath
                    ?.let {
                        fileUtilWrapper.getFileIfExists(fileName = it)?.toURI()?.toString()
                    },
                inShareOwnerContactItem = inShareOwnerContactItem,
                accessPermission = getNodeAccessPermission(typedNode.id) ?: AccessPermission.UNKNOWN
            )
        }
        updateHistory()
        updatePreview()
        updateFolderTreeInfo()
        updateOwner()
        updateOutShares()
        updateLocation()
    }

    private fun updateHistory() {
        if (typedNode is FileNode) {
            updateState { it.copy(historyVersions = getFileHistoryNumVersions(typedNode.id.longValue)) }
        }
    }

    private fun updatePreview() {
        viewModelScope.launch {
            if ((typedNode as? TypedFileNode)?.hasPreview == true && _uiState.value.previewUriString == null) {
                _uiState.update {
                    it.copy(previewUriString = getPreview(typedNode.id.longValue)?.uriStringIfExists())
                }
            }
        }
    }

    private fun updateFolderTreeInfo() {
        (typedNode as? TypedFolderNode)?.let { folder ->
            updateState {
                it.copy(folderTreeInfo = getFolderTreeInfo(folder))
            }
        }
    }

    private fun updateOwner() = (typedNode as? TypedFolderNode)?.let { folder ->
        updateState {
            it.copy(
                inShareOwnerContactItem = getContactItemFromInShareFolder(
                    folderNode = folder,
                    skipCache = true
                )
            )
        }
    }

    private fun updateAccessPermission() = updateState { viewState ->
        viewState.copy(
            accessPermission = getNodeAccessPermission(typedNode.id) ?: AccessPermission.UNKNOWN
        )
    }

    private fun updateLocation() {
        updateState {
            it.copy(nodeLocationInfo = getNodeLocationInfo(typedNode))
        }
    }

    private fun updateTitle() {
        updateState {
            it.copy(typedNode = getNodeById(typedNode.id))
        }
    }

    private fun updateOutShares() = updateState {
        it.copy(outShares = getOutShares(typedNode.id) ?: emptyList())
    }

    /**
     * Tries to move the node to the rubbish bin
     * It sets [FileInfoJobInProgressState.MovingToRubbish] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    private fun moveNodeToRubbishBin() {
        performBlockSettingProgress(FileInfoJobInProgressState.MovingToRubbish) {
            runCatching {
                moveNodeToRubbishByHandle(typedNode.id)
            }
        }
    }

    /**
     * Tries to delete the node
     * It sets [FileInfoJobInProgressState.Deleting] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    private fun deleteNode() {
        performBlockSettingProgress(FileInfoJobInProgressState.Deleting) {
            runCatching {
                deleteNodeByHandle(typedNode.id)
            }
        }
    }

    /**
     * Is connected
     */
    private val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Performs a job setting [progressState] state while in progress
     * First checks the device connection
     * @param progressState the [FileInfoJobInProgressState] that describes the job that will be done
     * @param block that performs the job
     */
    private fun performBlockSettingProgress(
        progressState: FileInfoJobInProgressState,
        block: suspend () -> Result<*>?,
    ) {
        if (checkAndHandleIsDeviceConnected()) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.update {
                    it.copy(jobInProgressState = progressState)
                }
                val result = block()
                // if there's a result, the job has finished, (for instance: collision detected returns null because it handles the ui update)
                if (result != null) {
                    _uiState.updateEventAndClearProgress(
                        FileInfoOneOffViewEvent.Finished(
                            jobFinished = progressState,
                            exception = result.exceptionOrNull()
                        ),
                    )
                }
            }
        }
    }

    /**
     * Checks if there is a name collision before moving or copying the node.
     *
     * @param parentHandle Parent handle of the node in which the node will be moved or copied.
     * @param type         Type of name collision to check.
     * @return true if there are no collision detected, so it can go ahead with the move or copy
     */
    private suspend fun checkCollision(parentHandle: NodeId, type: NameCollisionType) =
        try {
            val nameCollision = checkNameCollision(
                typedNode.id,
                parentHandle,
                type
            )
            _uiState.updateEventAndClearProgress(
                FileInfoOneOffViewEvent.CollisionDetected(
                    nameCollision
                )
            )
            false
        } catch (throwable: Throwable) {
            if (throwable is MegaNodeException.ChildDoesNotExistsException) {
                true
            } else {
                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.GeneralError)
                false
            }
        }

    private fun MutableStateFlow<FileInfoViewState>.updateEventAndClearProgress(event: FileInfoOneOffViewEvent?) =
        this.update {
            it.copy(
                oneOffViewEvent = event,
                jobInProgressState = null,
            )
        }

    private fun updateState(update: suspend (FileInfoViewState) -> FileInfoViewState) =
        viewModelScope.launch {
            _uiState.update {
                update(it)
            }
        }

    private fun File.uriStringIfExists() = this.takeIf { it.exists() }?.toURI()?.toString()
}