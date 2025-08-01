package mega.privacy.android.app.presentation.bottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetLegacyNodeWrapperUseCase
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeDeviceCenterInformation
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.app.utils.wrapper.LegacyNodeWrapper
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderNodeUseCase
import mega.privacy.android.domain.usecase.chat.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.contact.GetContactUserNameFromDatabaseUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeSyncedUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] associated with [NodeOptionsBottomSheetDialogFragment]
 *
 * @property createShareKeyUseCase [CreateShareKeyUseCase]
 * @property getNodeByIdUseCase [GetNodeByIdUseCase]
 * @property getNodeByHandle [GetNodeByHandle]
 * @property isNodeDeletedFromBackupsUseCase [IsNodeDeletedFromBackupsUseCase]
 * @property monitorConnectivityUseCase [MonitorConnectivityUseCase]
 * @property removeOfflineNodeUseCase [RemoveOfflineNodeUseCase]
 * @property getContactUserNameFromDatabaseUseCase [GetContactUserNameFromDatabaseUseCase]
 * @property updateNodeSensitiveUseCase [UpdateNodeSensitiveUseCase]
 * @property monitorAccountDetailUseCase [MonitorAccountDetailUseCase]
 * @property isHiddenNodesOnboardedUseCase [IsHiddenNodesOnboardedUseCase]
 * @property isHidingActionAllowedUseCase [IsHidingActionAllowedUseCase]
 * @property isAvailableOfflineUseCase [IsAvailableOfflineUseCase]
 * @property getBusinessStatusUseCase [GetBusinessStatusUseCase]
 * @property getCameraUploadsFolderHandleUseCase [GetPrimarySyncHandleUseCase]
 * @property getMediaUploadsFolderHandleUseCase [GetSecondaryFolderNodeUseCase]
 * @property getMyChatsFilesFolderIdUseCase [GetMyChatsFilesFolderIdUseCase]
 * @property isNodeSyncedUseCase [IsNodeSyncedUseCase]
 */
@HiltViewModel
class NodeOptionsViewModel @Inject constructor(
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val getLegacyNodeWrapperUseCase: GetLegacyNodeWrapperUseCase,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val getContactUserNameFromDatabaseUseCase: GetContactUserNameFromDatabaseUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getCameraUploadsFolderHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getMediaUploadsFolderHandleUseCase: GetSecondaryFolderNodeUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val isNodeSyncedUseCase: IsNodeSyncedUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shareKeyCreated = MutableStateFlow<Boolean?>(null)

    private val _state = MutableStateFlow(NodeBottomSheetUIState())

    /**
     * The UI State for [NodeOptionsBottomSheetDialogFragment]
     */
    val state = _state.asStateFlow()

    /**
     * Whether the device is online
     */
    fun isOnline() = state.value.isOnline

    init {
        viewModelScope.launch {
            combine(
                savedStateHandle.getStateFlow(NODE_ID_KEY, -1L).map {
                    val info = runCatching { getLegacyNodeWrapperUseCase(it) }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                    if (info != null) {
                        LegacyNodeWrapper(
                            node = info.node,
                            typedNode = info.typedNode,
                        ) to isAvailableOffline(info.typedNode)
                    } else null to false
                },
                savedStateHandle.getStateFlow(SHARE_DATA_KEY, null),
                savedStateHandle.getStateFlow(NODE_DEVICE_CENTER_INFORMATION_KEY, null),
                shareKeyCreated,
                monitorConnectivityUseCase(),
            ) { legacyNodeWrapperPair: Pair<LegacyNodeWrapper?, Boolean>, shareData: NodeShareInformation?, nodeDeviceCenterInformation: NodeDeviceCenterInformation?, shareKeyCreated: Boolean?, isOnline: Boolean ->
                { state: NodeBottomSheetUIState ->
                    state.copy(
                        legacyNodeWrapper = legacyNodeWrapperPair.first,
                        isAvailableOffline = legacyNodeWrapperPair.second,
                        shareData = shareData,
                        nodeDeviceCenterInformation = nodeDeviceCenterInformation,
                        shareKeyCreated = shareKeyCreated,
                        isOnline = isOnline,
                    )
                }
            }.collect {
                _state.update(it)
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
            runCatching {
                val nodeId = savedStateHandle.getStateFlow(NODE_ID_KEY, -1L).first()
                val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
                val isHidingActionAllowed =
                    isHidingActionAllowedUseCase(NodeId(nodeId))
                val isCameraUploadsFolder = nodeId == getCameraUploadsFolderHandleUseCase()
                val isMediaUploadsFolder =
                    nodeId == (getMediaUploadsFolderHandleUseCase()?.id?.longValue ?: false)
                val isMyChatFilesFolder = nodeId == getMyChatsFilesFolderIdUseCase()?.longValue
                val isSyncedFolder = isNodeSyncedUseCase(nodeId = NodeId(nodeId))
                _state.update {
                    it.copy(
                        isHiddenNodesOnboarded = isHiddenNodesOnboarded,
                        isHidingActionAllowed = isHidingActionAllowed,
                        isUserAttributeFolder = isCameraUploadsFolder || isMediaUploadsFolder || isMyChatFilesFolder,
                        isSyncedFolder = isSyncedFolder,
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private suspend fun isAvailableOffline(node: TypedNode) = runCatching {
        isAvailableOfflineUseCase(node)
    }.getOrDefault(false)

    /**
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    /**
     * Updates the UI State when the "Move" option is selected
     *
     * @param clicked true if the option is clicked, and false if otherwise
     */
    fun setMoveNodeClicked(clicked: Boolean) =
        _state.update { it.copy(canMoveNode = clicked) }

    /**
     * Updates the UI State when the "Restore" option is selected
     *
     * @param clicked true if the option is clicked, and false if otherwise
     */
    fun setRestoreNodeClicked(clicked: Boolean) = viewModelScope.launch {
        _state.value.legacyNodeWrapper?.let { info ->
            val isNodeDeletedFromBackups =
                isNodeDeletedFromBackupsUseCase(info.typedNode.id)
            if (isNodeDeletedFromBackups) {
                _state.update { it.copy(canMoveNode = clicked) }
            } else {
                _state.update { it.copy(canRestoreNode = clicked) }
            }
        }
    }

    /**
     * Creates a Shared Key
     */
    fun createShareKey() {
        viewModelScope.launch {
            runCatching {
                val nodeInfo = state.value.legacyNodeWrapper
                require(nodeInfo != null) { "Cannot create a share key for a null node" }
                require(nodeInfo.typedNode is FolderNode) {
                    "Cannot create a share key for a non-folder node"
                }
                createShareKeyUseCase(nodeInfo.typedNode)
            }.onSuccess {
                shareKeyCreated.emit(true)
            }.onFailure {
                shareKeyCreated.emit(false)
            }
        }
    }

    /**
     * Change the value of shareKeyCreated to false after it is consumed.
     */
    fun shareDialogDisplayed() {
        shareKeyCreated.tryEmit(null)
    }

    /**
     * Detect the file whether could be previewed directly
     *
     * @param node MegaNode
     * @return true is that the file could be previewed directly, otherwise is false
     */
    fun isFilePreviewOnline(node: MegaNode): Boolean =
        MimeTypeList.typeForName(node.name).let {
            it.isAudio || it.isVideoMimeType
        } && runCatching {
            (monitorStorageStateEventUseCase().value.storageState != StorageState.PayWall)
        }.getOrDefault(false)


    /**
     * Remove offline node
     */
    fun removeOfflineNode(nodeId: NodeId) {
        viewModelScope.launch {
            runCatching { removeOfflineNodeUseCase(nodeId) }
                .onFailure {
                    Timber.e(it)
                }
        }
    }

    fun hideOrUnhideNode(handle: Long, hidden: Boolean) {
        viewModelScope.launch {
            runCatching { updateNodeSensitiveUseCase(NodeId(handle), hidden) }
                .onFailure {
                    Timber.e(it)
                }
                .onSuccess {
                    Timber.d("Node $handle marked sensitive successfully.")
                }
        }
    }

    /**
     * Retrieves the Node Information of the Unverified Node with the Contact Name
     */
    suspend fun getUnverifiedOutgoingNodeUserName(): String? {
        val shareData = _state.value.shareData
        return shareData?.let { nonNullShareData ->
            getContactUserNameFromDatabaseUseCase(nonNullShareData.user)
        }
    }

    companion object {
        /**
         * The Mode Key
         */
        const val MODE_KEY = "MODE"

        /**
         * The Node ID Key
         */
        const val NODE_ID_KEY = "NODE_ID_KEY"

        /**
         * The Share Data Key
         */
        const val SHARE_DATA_KEY = "SHARE_DATA_KEY"

        /**
         * Key that holds specific information about a Device Center Node
         */
        const val NODE_DEVICE_CENTER_INFORMATION_KEY = "NODE_DEVICE_CENTER_INFORMATION_KEY"

        /**
         * Key that holds the value of the Hide Hidden Actions
         */
        const val HIDE_HIDDEN_ACTIONS_KEY = "HIDE_HIDDEN_ACTIONS_KEY"
    }
}