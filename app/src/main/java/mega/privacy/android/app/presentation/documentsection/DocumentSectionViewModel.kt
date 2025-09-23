package mega.privacy.android.app.presentation.documentsection

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.documentsection.model.DocumentSectionUiState
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNodeType
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.DocumentSectionHiddenNodeActionModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.DocumentSectionToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifierItem.DocumentSection
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.documentsection.GetAllDocumentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.navigation.ExtraConstant
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * The view model for the document section
 */
@HiltViewModel
class DocumentSectionViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val documentUiEntityMapper: DocumentUiEntityMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorViewType: MonitorViewType,
    private val setViewType: SetViewType,
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
    private val getRubbishBinFolderUseCase: GetRubbishBinFolderUseCase,
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentSectionUiState())
    internal val uiState = _uiState.asStateFlow()

    private val originalData = mutableListOf<DocumentUiEntity>()
    private var showHiddenItems: Boolean? = null
    private var actionModeStateJob: Job? = null

    /**
     * Is network connected
     */
    internal val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        checkViewType()
        viewModelScope.launch {
            if (isHiddenNodesActive()) {
                handleHiddenNodeUIFlow()
                monitorIsHiddenNodesOnboarded()
            } else {
                merge(
                    monitorNodeUpdatesUseCase(),
                    monitorOfflineNodeUpdatesUseCase()
                ).conflate()
                    .catch {
                        Timber.e(it)
                    }.collect {
                        refreshDocumentNodes()
                    }
            }
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    private fun handleHiddenNodeUIFlow() {
        combine(
            merge(
                monitorNodeUpdatesUseCase(),
                monitorOfflineNodeUpdatesUseCase(),
            ).conflate(),
            monitorAccountDetailUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { _, accountDetail, showHiddenItems ->
            this@DocumentSectionViewModel.showHiddenItems = showHiddenItems
            val accountType = accountDetail.levelDetail?.accountType
            val businessStatus =
                if (accountType?.isBusinessAccount == true) {
                    getBusinessStatusUseCase()
                } else null

            _uiState.update {
                it.copy(
                    accountType = accountType,
                    hiddenNodeEnabled = true,
                    isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                )
            }
            refreshDocumentNodes()
        }.catch {
            Timber.e(it)
        }.launchIn(viewModelScope)
    }

    private suspend fun filterNonSensitiveItems(
        items: List<DocumentUiEntity>,
        showHiddenItems: Boolean?,
        isPaid: Boolean?,
        isBusinessAccountExpired: Boolean,
    ) = withContext(defaultDispatcher) {
        showHiddenItems ?: return@withContext items
        isPaid ?: return@withContext items

        return@withContext if (showHiddenItems || !isPaid || isBusinessAccountExpired) {
            items
        } else {
            items.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
    }

    internal suspend fun refreshDocumentNodes() =
        runCatching {
            filterNonSensitiveItems(
                items = getDocumentUIEntityList(),
                showHiddenItems = showHiddenItems,
                isPaid = _uiState.value.accountType?.isPaid,
                isBusinessAccountExpired = _uiState.value.isBusinessAccountExpired,
            ).updateOriginalData()
        }.onSuccess { documentList ->
            val sortOrder = getCloudSortOrder()
            _uiState.update {
                it.copy(
                    allDocuments = documentList,
                    sortOrder = sortOrder,
                    isLoading = false,
                    scrollToTop = false,
                )
            }
        }.onFailure {
            Timber.e(it)
        }

    private suspend fun getDocumentUIEntityList(): List<DocumentUiEntity> {
        val rubbishBinNode = getRubbishBinFolderUseCase()
        return getAllDocumentsUseCase().map {
            val accessPermission =
                getNodeAccessUseCase(nodeId = it.id) ?: AccessPermission.UNKNOWN
            val canBeMovedToRubbishBin = rubbishBinNode != null && checkNodeCanBeMovedToTargetNode(
                nodeId = NodeId(longValue = it.id.longValue),
                targetNodeId = NodeId(longValue = rubbishBinNode.id.longValue)
            )
            documentUiEntityMapper(
                typedFileNode = it,
                accessPermission = accessPermission,
                canBeMovedToRubbishBin = canBeMovedToRubbishBin
            )
        }
    }

    private fun List<DocumentUiEntity>.updateOriginalData() = also { data ->
        originalData.clear()
        originalData.addAll(data)
    }

    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _uiState.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    internal fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_uiState.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            val sortOrder = getCloudSortOrder()
            _uiState.update {
                it.copy(
                    sortOrder = sortOrder,
                    isLoading = true
                )
            }
            refreshDocumentNodes()
        }

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    internal suspend fun getLocalFilePath(handle: Long): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = FileUtil.getLocalFile(node)
            val file = File(FileUtil.getDownloadLocation(), node.name)
            if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                        || (node.fingerprint == getFingerprintUseCase(localPath)))
            ) {
                localPath
            } else {
                null
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param fileType node file type
     * @param intent Intent
     * @return updated intent
     */
    internal suspend fun updateIntent(
        handle: Long,
        fileType: String,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        getFileUrlByNodeHandleUseCase(handle)?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, fileType)
            }
        }

        return intent
    }

    internal suspend fun getDocumentNodeByHandle(handle: Long) = getNodeByHandle(handle)

    internal suspend fun getSelectedMegaNode(): List<MegaNode> =
        _uiState.value.selectedNodes.mapNotNull {
            runCatching {
                getNodeByHandle(it.id)
            }.getOrNull()
        }

    internal fun clearAllSelectedDocuments() {
        val documents = clearDocumentsSelected()
        _uiState.update {
            it.copy(
                allDocuments = documents,
                selectedNodes = emptyList(),
                actionMode = false
            )
        }
    }

    private fun clearDocumentsSelected() = _uiState.value.allDocuments.map {
        it.copy(isSelected = false)
    }

    internal fun selectAllNodes() {
        val documents = _uiState.value.allDocuments.map { item ->
            item.copy(isSelected = true)
        }
        val selectedNodes = _uiState.value.allDocuments.map { item ->
            SelectedNode(
                id = item.id.longValue,
                type = SelectedNodeType.File,
                isTakenDown = item.isTakenDown,
                isExported = item.isExported,
                isIncomingShare = item.isIncomingShare,
                accessPermission = item.accessPermission,
                canBeMovedToRubbishBin = item.canBeMovedToRubbishBin
            )
        }
        _uiState.update {
            it.copy(
                allDocuments = documents,
                selectedNodes = selectedNodes,
                actionMode = true
            )
        }
        prepareActionMode()
    }

    internal fun setActionMode(value: Boolean) = _uiState.update { it.copy(actionMode = value) }

    internal fun onItemSelected(item: DocumentUiEntity, index: Int) =
        updateDocumentItemInSelectionState(item = item, index = index)

    private fun updateDocumentItemInSelectionState(item: DocumentUiEntity, index: Int) {
        val isSelected = !item.isSelected
        val selectedNodes = updateSelectedDocumentHandles(item, isSelected)
        val documents = _uiState.value.allDocuments.updateItemSelectedState(index, isSelected)
        _uiState.update {
            it.copy(
                allDocuments = documents,
                selectedNodes = selectedNodes,
                actionMode = selectedNodes.isNotEmpty()
            )
        }
        prepareActionMode()
    }

    private fun updateSelectedDocumentHandles(item: DocumentUiEntity, isSelected: Boolean) =
        _uiState.value.selectedNodes.toMutableList().also { selectedNodes ->
            if (isSelected) {
                selectedNodes.add(
                    SelectedNode(
                        id = item.id.longValue,
                        type = SelectedNodeType.File,
                        isTakenDown = item.isTakenDown,
                        isExported = item.isExported,
                        isIncomingShare = item.isIncomingShare,
                        accessPermission = item.accessPermission,
                        canBeMovedToRubbishBin = item.canBeMovedToRubbishBin
                    )
                )
            } else {
                selectedNodes.removeAll { it.id == item.id.longValue }
            }
        }

    private fun List<DocumentUiEntity>.updateItemSelectedState(
        index: Int,
        isSelected: Boolean,
    ) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this

    private fun prepareActionMode() {
        actionModeStateJob?.cancel()
        actionModeStateJob = viewModelScope.launch {
            val selectedNodes = _uiState.value.allDocuments.filter { audio ->
                _uiState.value.selectedNodes.firstOrNull { selectedNode ->
                    audio.id.longValue == selectedNode.id
                } != null
            }
            if (selectedNodes.isEmpty()) {
                _uiState.update { it.copy(toolbarActionsModifierItem = null) }
                return@launch
            }

            val hiddenNodeActionModeState = runCatching {
                getHiddenNodeActionModifierItem(selectedNodes = selectedNodes)
            }.getOrDefault(defaultValue = DocumentSectionHiddenNodeActionModifierItem())
            _uiState.update {
                it.copy(
                    toolbarActionsModifierItem = DocumentSection(
                        item = DocumentSectionToolbarActionsModifierItem(
                            hiddenNodeItem = hiddenNodeActionModeState
                        )
                    )
                )
            }
        }
    }

    private suspend fun getHiddenNodeActionModifierItem(selectedNodes: List<DocumentUiEntity>): DocumentSectionHiddenNodeActionModifierItem {
        val isHiddenNodesEnabled = isHiddenNodesActive()
        if (!isHiddenNodesEnabled) {
            return DocumentSectionHiddenNodeActionModifierItem(isEnabled = false)
        }

        val isPaid = _uiState.value.accountType?.isPaid ?: false
        if (!isPaid || _uiState.value.isBusinessAccountExpired) {
            return DocumentSectionHiddenNodeActionModifierItem(
                isEnabled = true,
                canBeHidden = true
            )
        }

        val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }
        val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }
        return DocumentSectionHiddenNodeActionModifierItem(
            isEnabled = true,
            canBeHidden = hasNonSensitiveNode && !includeSensitiveInheritedNode
        )
    }

    internal fun hideOrUnhideNodes(nodeIds: List<NodeId>, hide: Boolean) =
        viewModelScope.launch {
            for (nodeId in nodeIds) {
                async {
                    runCatching {
                        updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
                    }.onFailure { Timber.e("Update sensitivity failed: $it") }
                }
            }
        }

    private suspend fun monitorIsHiddenNodesOnboarded() {
        val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
        _uiState.update {
            it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
        }
    }

    /**
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _uiState.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }
}