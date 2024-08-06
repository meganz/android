package mega.privacy.android.app.presentation.fileinfo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.shares.GetOutShares
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoExtraAction
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoJobInProgressState
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoOneOffViewEvent
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.app.presentation.fileinfo.model.mapper.NodeActionMapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent.StartDownloadForOffline
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.data.gateway.ClipboardGateway
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges.Description
import mega.privacy.android.domain.entity.node.NodeChanges.Inshare
import mega.privacy.android.domain.entity.node.NodeChanges.Name
import mega.privacy.android.domain.entity.node.NodeChanges.Outshare
import mega.privacy.android.domain.entity.node.NodeChanges.Owner
import mega.privacy.android.domain.entity.node.NodeChanges.Parent
import mega.privacy.android.domain.entity.node.NodeChanges.Public_link
import mega.privacy.android.domain.entity.node.NodeChanges.Remove
import mega.privacy.android.domain.entity.node.NodeChanges.Tags
import mega.privacy.android.domain.entity.node.NodeChanges.Timestamp
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetImageNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.MonitorChildrenUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.MonitorOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.contact.MonitorChatOnlineStatusUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeByHandleUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.filenode.GetNodeVersionsByHandleUseCase
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.GetAvailableNodeActionsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.SetNodeDescriptionUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.GetNodeOutSharesUseCase
import mega.privacy.android.domain.usecase.shares.SetOutgoingPermissions
import mega.privacy.android.domain.usecase.shares.StopSharingNode
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * View Model class for [FileInfoActivity]
 */
@HiltViewModel
class FileInfoViewModel @Inject constructor(
    private val tempMegaNodeRepository: MegaNodeRepository, //a temp use of MegaApiAndroid, just while migrating to use-cases only, to easily remove things from the activity
    private val fileUtilWrapper: FileUtilWrapper,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
    private val deleteNodeByHandleUseCase: DeleteNodeByHandleUseCase,
    private val deleteNodeVersionsUseCase: DeleteNodeVersionsUseCase,
    private val getPreviewUseCase: GetPreviewUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFolderTreeInfo: GetFolderTreeInfo,
    private val getContactItemFromInShareFolder: GetContactItemFromInShareFolder,
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById,
    private val monitorChildrenUpdates: MonitorChildrenUpdates,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val monitorChatOnlineStatusUseCase: MonitorChatOnlineStatusUseCase,
    private val getNodeVersionsByHandleUseCase: GetNodeVersionsByHandleUseCase,
    private val getOutShares: GetOutShares,
    private val getNodeOutSharesUseCase: GetNodeOutSharesUseCase,
    private val getNodeLocationInfo: GetNodeLocationInfo,
    private val setNodeDescriptionUseCase: SetNodeDescriptionUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val setOutgoingPermissions: SetOutgoingPermissions,
    private val stopSharingNode: StopSharingNode,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val getAvailableNodeActionsUseCase: GetAvailableNodeActionsUseCase,
    private val nodeActionMapper: NodeActionMapper,
    private val clipboardGateway: ClipboardGateway,
    private val monitorOfflineFileAvailabilityUseCase: MonitorOfflineFileAvailabilityUseCase,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val getAccountTypeUseCase: GetAccountTypeUseCase,
    private val isBusinessAccountActive: IsBusinessAccountActive,
    private val getImageNodeByNodeId: GetImageNodeByIdUseCase,
    @IoDispatcher private val iODispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileInfoViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

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
     * the [NodeId] of the current node for this screen
     */
    val nodeId get() = typedNode.id

    init {
        checkDescriptionFlag()
        checkMapLocationFeatureFlag()
        checkTagsFeatureFlag()
        checkAccountStatus()
        viewModelScope.launch {
            val isRemindersForContactVerificationEnabled =
                getContactVerificationWarningUseCase()
            _uiState.update { it.copy(isRemindersForContactVerificationEnabled = isRemindersForContactVerificationEnabled) }
        }
    }

    private fun checkAccountStatus() = viewModelScope.launch {
        runCatching {
            getAccountTypeUseCase()
        }.onSuccess { result ->
            if (result == AccountType.BUSINESS) {
                runCatching {
                    isBusinessAccountActive()
                }.onSuccess { isBusinessAccountActive ->
                    _uiState.update { it.copy(isBusinessAccountActive = isBusinessAccountActive) }
                }.onFailure {
                    Timber.e("Get isBusinessAccountActive failed with error: $it")
                }
            } else {
                _uiState.update { it.copy(isProAccount = result != AccountType.FREE) }
            }
        }.onFailure {
            Timber.e("Get getAccountType failed with error: $it")
        }
    }

    private fun checkTagsFeatureFlag() = viewModelScope.launch {
        runCatching { getFeatureFlagValueUseCase(AppFeatures.NodeWithTags) }.onSuccess { flag ->
            _uiState.update { it.copy(tagsEnabled = flag) }
        }.onFailure {
            Timber.e("Get tag feature flag failed $it")
        }
    }

    private fun checkMapLocationFeatureFlag() = viewModelScope.launch {
        runCatching { getFeatureFlagValueUseCase(AppFeatures.MapLocation) }.onSuccess { flag ->
            _uiState.update { it.copy(mapLocationEnabled = flag) }
        }.onFailure {
            Timber.e("Get gis feature flag failed $it")
        }
    }

    private fun checkDescriptionFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.NodeWithDescription)
            }.onSuccess { flag ->
                _uiState.update {
                    it.copy(nodeDescriptionEnabled = flag)
                }
            }.onFailure {
                Timber.e("Get feature flag failed $it")
            }
        }
    }

    /**
     * Set a node description
     */
    fun setNodeDescription(description: String) {
        viewModelScope.launch {
            runCatching {
                setNodeDescriptionUseCase(nodeHandle = nodeId, description = description)
            }.onSuccess {
                _uiState.update {
                    if (it.descriptionText.isEmpty()) {
                        it.copy(
                            oneOffViewEvent = triggered(FileInfoOneOffViewEvent.Message.NodeDescriptionAdded),
                            descriptionText = description
                        )
                    } else {
                        it.copy(
                            oneOffViewEvent = triggered(FileInfoOneOffViewEvent.Message.NodeDescriptionUpdated),
                            descriptionText = description
                        )
                    }
                }
            }.onFailure {
                Timber.e("Set Node Description Failed $it")
            }
        }
    }

    /**
     * Sets the node and updates its state
     */
    fun setNode(handleNode: Long, forceUpdate: Boolean = false) {
        if (this::typedNode.isInitialized && handleNode == nodeId.longValue && !forceUpdate) {
            //do not need to update on screen rotation, for instance
            Timber.d("No need to update as it's the same node $handleNode")
            return
        }
        Timber.d("FileInfoViewModel node set $handleNode")
        viewModelScope.launch {
            runCatching {
                typedNode = getNodeByIdUseCase(NodeId(handleNode))
                    ?: throw RuntimeException()
                node = tempMegaNodeRepository.getNodeByHandle(handleNode)
                    ?: throw RuntimeException()
            }.onFailure {
                Timber.e("FileInfoViewModel node not found")
                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NodeDeleted)
                return@launch
            }

            if (typedNode is FileNode) {
                val parent = getNodeByIdUseCase(typedNode.parentId)
                if (parent is FileNode) {
                    //we only want the latest version in this screen
                    setNode(parent.id.longValue)
                    return@launch
                }
                versions = getNodeVersionsByHandleUseCase(typedNode.id)
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
                        monitorOnlineState(),
                        monitorOfflineUpdates()
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
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(typedNode.id.longValue to parentHandle.longValue),
                    type = NodeNameCollisionType.MOVE
                ).also {
                    if (it.moveRequestResult?.isAllRequestError == true) {
                        throw IllegalStateException("Move request failed")
                    }
                }
            }.onSuccess {
                it.firstNodeCollisionOrNull?.let { collision ->
                    _uiState.updateEventAndClearProgress(
                        FileInfoOneOffViewEvent.CollisionDetected(collision)
                    )
                    return@performBlockSettingProgress null
                }
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
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(typedNode.id.longValue to parentHandle.longValue),
                    type = NodeNameCollisionType.COPY
                ).also {
                    if (it.moveRequestResult?.isAllRequestError == true) {
                        throw IllegalStateException("Copy request failed")
                    }
                }
            }.onSuccess {
                it.firstNodeCollisionOrNull?.let { collision ->
                    _uiState.updateEventAndClearProgress(
                        FileInfoOneOffViewEvent.CollisionDetected(collision)
                    )
                    return@performBlockSettingProgress null
                }
            }
        }

    /**
     * It checks if the node is in the rubbish bin,
     * if it's not in the rubbish bin, moves the node to the rubbish bin
     * if it's already in the rubbish bin, deletes the node.
     * It will sets the proper [FileInfoJobInProgressState] and launch the proper [FileInfoOneOffViewEvent.Finished]
     */
    internal fun removeNode() {
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
                deleteNodeVersionsUseCase(typedNode.id)
            }
        }
    }


    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent() {
        _uiState.updateEventAndClearProgress(null)
    }

    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeDownloadEvent() {
        _uiState.updateDownloadEvent(null)
    }

    /**
     * Change the current offline availability
     */
    fun availableOfflineChanged(
        availableOffline: Boolean,
    ) {
        viewModelScope.launch {
            if (availableOffline == _uiState.value.isAvailableOffline) return@launch
            if (availableOffline && monitorStorageStateEventUseCase.getState() == StorageState.PayWall) {
                updateState { it.copy(oneOffViewEvent = triggered(FileInfoOneOffViewEvent.OverDiskQuota)) }
                return@launch
            }
            if (availableOffline) {
                updateState {
                    it.copy(
                        downloadEvent = triggered(StartDownloadForOffline(typedNode)),
                        isAvailableOffline = true,
                    )
                }
            } else {
                updateState {
                    it.copy(isAvailableOfflineEnabled = false) // to avoid multiple changes while changing
                }
                viewModelScope.launch {
                    removeOfflineNodeUseCase(
                        typedNode.id
                    )
                    updateState {
                        it.copy(
                            oneOffViewEvent = triggered(FileInfoOneOffViewEvent.Message.RemovedOffline),
                            isAvailableOffline = false,
                            isAvailableOfflineEnabled = !typedNode.isTakenDown && !it.isNodeInRubbish,
                        )
                    }
                }
            }
        }
    }

    /**
     * Stop sharing this node
     */
    fun stopSharing() {
        viewModelScope.launch {
            stopSharingNode(typedNode.id)
        }
    }

    /**
     * A contact is selected to show Sharing info
     */
    fun contactToShowOptions(email: String?) {
        updateState {
            it.copy(
                contactToShowOptions = email,
                outShareContactsSelected = emptyList(),
            )
        }
    }

    /**
     * gets the [MegaShare] of the given email if it exists
     */
    @Deprecated("this should be avoided, need while using FileContactsListBottomSheetDialogFragment. To be removed once migrated to compose")
    fun getShareFromEmail(email: String?): MegaShare? =
        _uiState.value.outSharesDeprecated.firstOrNull { it.user == email }

    /**
     * A new contact of the shared list are selected
     */
    fun contactSelectedInSharedList(email: String) {
        updateState {
            it.copy(
                outShareContactsSelected = it.outShareContactsSelected + email,
                contactToShowOptions = null,
            )
        }
    }

    /**
     * A contact of the shared list are unselected
     */
    fun contactUnselectedInSharedList(email: String) {
        updateState {
            it.copy(
                outShareContactsSelected = it.outShareContactsSelected - email,
                contactToShowOptions = null,
            )
        }
    }

    /**
     * All visible contacts of the shared list are selected
     */
    fun selectAllVisibleContacts() =
        updateState { viewState ->
            viewState.copy(
                outShareContactsSelected = _uiState.value.outSharesCoerceMax.map { it.contactItem.email },
                contactToShowOptions = null,
            )
        }

    /**
     * unselect all contacts of the shared list
     */
    fun unselectAllContacts() {
        updateState {
            it.copy(
                outShareContactsSelected = emptyList(),
                contactToShowOptions = null,
            )
        }
    }

    /**
     * Set out sharing permission for contacts in [emails] list.
     */
    fun setSharePermissionForUsers(accessPermission: AccessPermission, emails: List<String>) {
        extraActionFinished()
        val alreadySet = _uiState.value.outShares.map { it.contactItem.email }.containsAll(emails)
        changeSharePermissionForUsers(
            accessPermission,
            if (alreadySet) FileInfoJobInProgressState.ChangeSharePermission.Change else FileInfoJobInProgressState.ChangeSharePermission.Set,
            *emails.toTypedArray()
        )
    }

    /**
     * Removes permission for contacts in [emails]
     */
    fun removeSharePermissionForUsers(vararg emails: String) =
        changeSharePermissionForUsers(
            AccessPermission.UNKNOWN,
            FileInfoJobInProgressState.ChangeSharePermission.Remove,
            *emails
        )

    /**
     * Copies the public link to the clipboard and send the related event
     */
    fun copyPublicLink() {
        Timber.d("Copy link button")
        _uiState.value.publicLink?.let {
            clipboardGateway.setClip(Constants.COPIED_TEXT_LABEL, it)
            _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.PublicLinkCopiedToClipboard)
        }
    }

    /**
     * Updates the uiState to confirm remove shared contacts
     */
    fun initiateRemoveContacts(emails: List<String>) {
        if (!checkAndHandleIsDeviceConnected()) {
            return
        }
        _uiState.update {
            it.copy(
                requiredExtraAction = FileInfoExtraAction.ConfirmRemove.DeleteContact(emails)
            )
        }
    }

    /**
     * Updates the uiState to confirm remove shared contacts
     */
    fun initiateChangePermission(
        emails: List<String>?,
    ) {
        if (!checkAndHandleIsDeviceConnected()) {
            return
        }
        val actualEmails = emails ?: _uiState.value.outShareContactsSelected
        val actualPermissions = actualEmails.map { email ->
            _uiState.value.outShares.firstOrNull {
                it.contactItem.email == email
            }?.accessPermission ?: AccessPermission.UNKNOWN
        }
        //if all current permissions are the same, we set it as selected one
        val selected = actualPermissions.firstOrNull()
            ?.takeIf { candidate -> actualPermissions.all { it == candidate } }

        _uiState.update {
            it.copy(
                requiredExtraAction = FileInfoExtraAction.ChangePermission(actualEmails, selected)
            )
        }
    }

    /**
     * Updates the uiState to confirm remove shared contacts
     */
    fun initiateRemoveLink() {
        if (!checkAndHandleIsDeviceConnected()) {
            return
        }
        _uiState.update {
            it.copy(requiredExtraAction = FileInfoExtraAction.ConfirmRemove.DeleteLink)
        }
    }


    /**
     * Updates the uiState to confirm remove node
     */
    fun initiateRemoveNode(sendToRubbish: Boolean) {
        if (!checkAndHandleIsDeviceConnected()) {
            return
        }
        viewModelScope.launch {
            val requiresAction =
                if (sendToRubbish) {
                    val handle = nodeId.longValue
                    when {
                        getPrimarySyncHandleUseCase() == handle && isCameraUploadsEnabledUseCase() -> {
                            FileInfoExtraAction.ConfirmRemove.SendToRubbishCameraUploads
                        }

                        getSecondarySyncHandleUseCase() == handle && isMediaUploadsEnabledUseCase() -> {
                            FileInfoExtraAction.ConfirmRemove.SendToRubbishSecondaryMediaUploads
                        }

                        else -> FileInfoExtraAction.ConfirmRemove.SendToRubbish
                    }
                } else {
                    FileInfoExtraAction.ConfirmRemove.Delete
                }

            _uiState.update {
                it.copy(requiredExtraAction = requiresAction)
            }
        }
    }

    /**
     * User has confirmed a remove (something) action, so it will be done
     */
    fun removeConfirmed() {
        (_uiState.value.requiredExtraAction as? FileInfoExtraAction.ConfirmRemove)?.let {
            extraActionFinished()
            when (it) {
                is FileInfoExtraAction.ConfirmRemove.RemoveNode -> removeNode()
                is FileInfoExtraAction.ConfirmRemove.DeleteContact -> {
                    removeSharePermissionForUsers(*it.emails.toTypedArray())
                }

                is FileInfoExtraAction.ConfirmRemove.DeleteLink -> stopSharing()
            }
        }
    }

    /**
     * The required extra user action has finished, either done or cancelled
     */
    fun extraActionFinished() {
        _uiState.update { it.copy(requiredExtraAction = null) }
    }

    /**
     * @return true if the node is available offline
     */
    fun isAvailableOffline() = _uiState.value.isAvailableOffline

    /**
     * @return true if the node is a file
     */
    fun isFile() = typedNode is FileNode
    private fun changeSharePermissionForUsers(
        accessPermission: AccessPermission,
        progress: FileInfoJobInProgressState.ChangeSharePermission,
        vararg emails: String,
    ) = (typedNode as? TypedFolderNode)?.let { folderNode ->
        //clear selection to update related UI state (close bottom sheet, etc.)
        _uiState.update {
            it.copy(
                outShareContactsSelected = emptyList(),
                contactToShowOptions = null,
            )
        }
        //update permissions
        this.performBlockSettingProgress(progress) {
            runCatching {
                setOutgoingPermissions(folderNode, accessPermission, *emails)
            }.onFailure {
                Timber.e("FileInfoViewModel changeSharePermissionForUsers error $it")
            }
        }
    }

    private fun monitorNodeUpdates() =
        viewModelScope.launch {
            monitorNodeUpdatesById(typedNode.id).collect { changes ->
                //node has been updated
                Timber.d("FileInfoViewModel monitorNodeUpdates $changes")
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

                        Owner -> updateOwner()
                        Inshare -> updateAccessPermission()
                        Parent ->
                            if (!_uiState.value.isNodeInRubbish
                                && isNodeInRubbishBinUseCase(typedNode.id)
                            ) {
                                //if the node is moved to rubbish bin, let's close the screen like when it's deleted
                                _uiState.updateEventAndClearProgress(FileInfoOneOffViewEvent.NodeDeleted)
                            } else {
                                // maybe it's moved, or a new version is created, etc. let's update all to be safe
                                return@any true
                            }

                        Description, Name, Tags, Timestamp -> updateTypedNode()
                        Outshare -> {
                            updateOutShares()
                            updateIcon()
                        }

                        Public_link -> return@any true //will update only public link, for now update everything
                        else -> return@any false
                    }
                    return@any false
                }
                if (updateNode) {
                    setNode(typedNode.id.longValue, forceUpdate = true)
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

    private fun monitorOnlineState() =
        viewModelScope.launch {
            monitorChatOnlineStatusUseCase().filter { onlineStatus ->
                _uiState.value.outShares.any { it.contactItem.handle == onlineStatus.userHandle }
            }.collect { onlineStatus ->
                updateState { uiState ->
                    uiState.outShares.indexOfFirst {
                        it.contactItem.handle == onlineStatus.userHandle
                    }.takeIf { it >= 0 }?.let { contactUpdatedIndex ->
                        // lets update the online status of the corresponding user in outShares
                        val outShares = uiState.outShares.toMutableList().apply {
                            this[contactUpdatedIndex] = this[contactUpdatedIndex].let {
                                it.copy(contactItem = it.contactItem.copy(status = onlineStatus.status))
                            }
                        }
                        uiState.copy(outShares = outShares)
                    }
                        ?: uiState //this should not happen because we filtered the list, but a race condition can happen
                }
            }
        }

    private fun monitorOfflineUpdates() = viewModelScope.launch {
        monitorOfflineFileAvailabilityUseCase().filter { it == nodeId.longValue }.collect {
            _uiState.update {
                it.copy(isAvailableOffline = isAvailableOfflineUseCase(typedNode))
            }
        }
    }

    private fun updateCurrentNodeStatus() {
        updateState { uiState ->
            val inShareOwnerContactItem = (typedNode as? TypedFolderNode)?.let {
                //a first cached fast version, later we'll ask for a fresh ContactItem
                getContactItemFromInShareFolder(folderNode = it, skipCache = false)
            }
            val isNodeInRubbish = isNodeInRubbishBinUseCase(typedNode.id)
            uiState.copyWithTypedNode(
                typedNode = typedNode,
            ).copy(
                iconResource = getNodeIcon(
                    typedNode = typedNode,
                    originShares = _uiState.value.origin.fromShares,
                    fileTypeIconMapper = fileTypeIconMapper
                ),
                isNodeInBackups = isNodeInBackupsUseCase(typedNode.id.longValue),
                isNodeInRubbish = isNodeInRubbish,
                jobInProgressState = null,
                isAvailableOffline = isAvailableOfflineUseCase(typedNode),
                isAvailableOfflineEnabled = !typedNode.isTakenDown && !isNodeInRubbish,
                thumbnailUriString = (typedNode as? TypedFileNode)?.thumbnailPath
                    ?.let {
                        fileUtilWrapper.getFileIfExists(fileName = it)?.toURI()?.toString()
                    },
                inShareOwnerContactItem = inShareOwnerContactItem,
                accessPermission = getNodeAccessPermission(typedNode.id)
                    ?: AccessPermission.UNKNOWN
            )
        }
        updateHistory()
        updatePreview()
        updateFolderTreeInfo()
        updateOwner()
        updateOutShares()
        updateLocation()
        updateMapLocationInfo()
    }

    private fun updateHistory() {
        (typedNode as? FileNode)?.let { fileNode ->
            updateState { it.copy(historyVersions = fileNode.versionCount) }
        }
    }

    private fun updateIcon() {
        updateState {
            //we need to update the typedNode to get changes, for instance, on outgoing shares
            getNodeByIdUseCase(typedNode.id)?.let { updateTypedNode ->
                typedNode = updateTypedNode
            }
            it.copyWithTypedNode(
                typedNode = typedNode
            ).copy(
                iconResource = getNodeIcon(
                    typedNode = typedNode,
                    originShares = _uiState.value.origin.fromShares,
                    fileTypeIconMapper = fileTypeIconMapper
                ),
            )
        }
    }

    private fun updatePreview() {
        viewModelScope.launch {
            if ((typedNode as? TypedFileNode)?.hasPreview == true && _uiState.value.previewUriString == null) {
                runCatching {
                    getPreviewUseCase(typedNode)
                }.onSuccess { previewUri ->
                    _uiState.update {
                        it.copy(previewUriString = previewUri?.uriStringIfExists())
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private fun updateFolderTreeInfo() {
        (typedNode as? TypedFolderNode)?.let { folder ->
            viewModelScope.launch {
                runCatching {
                    getFolderTreeInfo(folder).let { folderTreeInfo ->
                        _uiState.update {
                            it.copyWithFolderTreeInfo(folderTreeInfo)
                        }
                    }
                }.onFailure {
                    Timber.w("Exception getting folder tree info.", it)
                }
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

    private fun isPhoto(): Boolean {
        return (typedNode as? TypedFileNode)?.type.let { fileType ->
            isFile() && (fileType is ImageFileTypeInfo || fileType is VideoFileTypeInfo)
        }
    }

    private fun updateMapLocationInfo() = viewModelScope.launch {
        if (isPhoto()) {
            runCatching {
                getImageNodeByNodeId(typedNode.id)?.let { imageNode ->
                    _uiState.update {
                        it.copy(
                            longitude = imageNode.longitude,
                            latitude = imageNode.latitude,
                            isPhoto = true,
                        )
                    }
                } ?: _uiState.update {
                    it.copy(isPhoto = false)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        } else {
            _uiState.update {
                it.copy(isPhoto = false)
            }
        }
    }

    private fun updateTypedNode() {
        updateState {
            getNodeByIdUseCase(typedNode.id)?.let { updateTypedNode ->
                typedNode = updateTypedNode
            }
            it.copyWithTypedNode(typedNode = typedNode)
        }
    }

    private fun updateOutShares() = updateState {
        it.copy(
            outSharesDeprecated = getOutShares(typedNode.id) ?: emptyList(),
            outShares = getNodeOutSharesUseCase(typedNode.id),
        )
    }

    /**
     * Tries to move the node to the rubbish bin
     * It sets [FileInfoJobInProgressState.MovingToRubbish] while moving.
     * It will launch [FileInfoOneOffViewEvent.Finished] at the end, with an exception if something went wrong
     */
    private fun moveNodeToRubbishBin() {
        performBlockSettingProgress(FileInfoJobInProgressState.MovingToRubbish) {
            runCatching {
                moveNodeToRubbishBinUseCase(typedNode.id)
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
                deleteNodeByHandleUseCase(typedNode.id)
            }
        }
    }

    /**
     * Is connected
     */
    private val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

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
            _uiState.update {
                it.copy(jobInProgressState = progressState)
            }
            viewModelScope.launch {
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

    private fun MutableStateFlow<FileInfoViewState>.updateEventAndClearProgress(event: FileInfoOneOffViewEvent?) =
        this.update {
            it.copy(
                oneOffViewEvent = event?.let { triggered(event) } ?: consumed(),
                jobInProgressState = null,
            )
        }

    private fun MutableStateFlow<FileInfoViewState>.updateDownloadEvent(event: TransferTriggerEvent?) =
        this.update {
            it.copy(downloadEvent = event?.let { triggered(event) } ?: consumed())
        }

    private fun updateState(update: suspend (FileInfoViewState) -> FileInfoViewState) =
        viewModelScope.launch {
            _uiState.update {
                update(it).copy(actions = getAvailableNodeActionsUseCase(typedNode).map { nodeAction ->
                    nodeActionMapper(nodeAction)
                })
            }
        }

    private fun File.uriStringIfExists() = this.takeIf { it.exists() }?.toURI()?.toString()

    /**
     * It checks the feature flag and start downloading the node with the appropriate use case or launch an one off event to start legacy download
     */
    fun startDownloadNode() {
        viewModelScope.launch {
            _uiState.updateDownloadEvent(
                TransferTriggerEvent.StartDownloadNode(listOf(typedNode))
            )
        }
    }

    suspend fun getAddress(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): Address? = withContext(iODispatcher) {
        try {
            val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) {
                        if (cont.isActive) {
                            cont.resume(it.firstOrNull())
                        }
                    }
                }
            } else {
                suspendCancellableCoroutine { cont ->
                    @Suppress("DEPRECATION")
                    val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    if (cont.isActive) {
                        cont.resume(address)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}
