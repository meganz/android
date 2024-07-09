package mega.privacy.android.app.presentation.audiosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.audiosection.mapper.AudioUiEntityMapper
import mega.privacy.android.app.presentation.audiosection.model.AudioSectionState
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.audiosection.GetAllAudioUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for audio section
 */
@HiltViewModel
class AudioSectionViewModel @Inject constructor(
    private val getAllAudioUseCase: GetAllAudioUseCase,
    private val audioUIEntityMapper: AudioUiEntityMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AudioSectionState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<AudioSectionState> = _state.asStateFlow()

    private var searchQuery = ""
    private val originalData = mutableListOf<TypedAudioNode>()
    private val originalEntities = mutableListOf<AudioUiEntity>()
    private var showHiddenItems: Boolean? = null

    init {
        checkViewType()
        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
                handleHiddenNodesUIFlow()
                monitorIsHiddenNodesOnboarded()
            } else {
                merge(
                    monitorNodeUpdatesUseCase(),
                    monitorOfflineNodeUpdatesUseCase()
                ).conflate()
                    .catch { Timber.e(it) }
                    .collect {
                        setPendingRefreshNodes()
                    }
            }
        }
    }

    private fun handleHiddenNodesUIFlow() {
        combine(
            merge(
                monitorNodeUpdatesUseCase(),
                monitorOfflineNodeUpdatesUseCase(),
            ).conflate(),
            monitorAccountDetailUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { _, accountDetail, showHiddenItems ->
            this@AudioSectionViewModel.showHiddenItems = showHiddenItems
            _state.update {
                it.copy(
                    accountDetail = accountDetail,
                    isPendingRefresh = true
                )
            }
        }.catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    private suspend fun filterNonSensitiveItems(
        items: List<AudioUiEntity>,
        showHiddenItems: Boolean?,
        isPaid: Boolean?,
    ) = withContext(defaultDispatcher) {
        showHiddenItems ?: return@withContext items
        isPaid ?: return@withContext items

        return@withContext if (showHiddenItems || !isPaid) {
            items
        } else {
            items.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
    }

    private fun setPendingRefreshNodes() = _state.update { it.copy(isPendingRefresh = true) }

    internal fun refreshNodes() = viewModelScope.launch {
        val audioList = filterNonSensitiveItems(
            items = getAudioUiEntityList(),
            showHiddenItems = this@AudioSectionViewModel.showHiddenItems,
            isPaid = _state.value.accountDetail?.levelDetail?.accountType?.isPaid,
        ).updateOriginalEntities().filterAudiosBySearchQuery()

        val sortOrder = getCloudSortOrder()
        _state.update {
            it.copy(
                allAudios = audioList,
                sortOrder = sortOrder,
                progressBarShowing = false,
                scrollToTop = false,
                isPendingRefresh = false
            )
        }
    }

    private suspend fun getAudioUiEntityList() =
        getAllAudioUseCase().updateOriginalData().map { audioUIEntityMapper(it) }

    private fun List<TypedAudioNode>.updateOriginalData() = also { data ->
        if (originalData.isNotEmpty()) {
            originalData.clear()
        }
        originalData.addAll(data)
    }

    private fun List<AudioUiEntity>.filterAudiosBySearchQuery() =
        filter { audio ->
            audio.name.contains(searchQuery, true)
        }

    private fun List<AudioUiEntity>.updateOriginalEntities() = also { entities ->
        if (originalEntities.isNotEmpty()) {
            originalEntities.clear()
        }
        originalEntities.addAll(entities)
    }

    internal fun markHandledPendingRefresh() = _state.update { it.copy(isPendingRefresh = false) }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            val sortOrder = getCloudSortOrder()
            _state.update {
                it.copy(
                    sortOrder = sortOrder,
                    progressBarShowing = true
                )
            }
            refreshNodes()
        }

    internal fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    internal fun clearAllSelectedAudios() {
        val audios = clearAudiosSelected()
        _state.update {
            it.copy(
                allAudios = audios,
                selectedAudioHandles = emptyList(),
                isInSelection = false
            )
        }
    }

    private fun clearAudiosSelected() = _state.value.allAudios.map {
        it.copy(isSelected = false)
    }

    internal fun selectAllNodes() {
        val audios = _state.value.allAudios.map { item ->
            item.copy(isSelected = true)
        }
        val selectedHandles = _state.value.allAudios.map { item ->
            item.id.longValue
        }
        _state.update {
            it.copy(
                allAudios = audios,
                selectedAudioHandles = selectedHandles,
                isInSelection = true
            )
        }
    }

    internal fun onItemClicked(item: AudioUiEntity, index: Int) {
        if (_state.value.isInSelection) {
            updateAudioItemInSelectionState(item = item, index = index)
        } else {
            updateClickedItem(getTypedAudioNodeById(item.id))
        }
    }

    internal fun onItemLongClicked(item: AudioUiEntity, index: Int) =
        updateAudioItemInSelectionState(item = item, index = index)

    private fun updateAudioItemInSelectionState(item: AudioUiEntity, index: Int) {
        val isSelected = !item.isSelected
        val selectedHandles = updateSelectedAudioHandles(item, isSelected)
        val audios = _state.value.allAudios.updateItemSelectedState(index, isSelected)
        _state.update {
            it.copy(
                allAudios = audios,
                selectedAudioHandles = selectedHandles,
                isInSelection = selectedHandles.isNotEmpty()
            )
        }
    }

    private fun List<AudioUiEntity>.updateItemSelectedState(index: Int, isSelected: Boolean) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this


    private fun updateSelectedAudioHandles(item: AudioUiEntity, isSelected: Boolean) =
        _state.value.selectedAudioHandles.toMutableList().also { selectedHandles ->
            if (isSelected) {
                selectedHandles.add(item.id.longValue)
            } else {
                selectedHandles.remove(item.id.longValue)
            }
        }

    internal fun shouldShowSearchMenu() = _state.value.allAudios.isNotEmpty()

    internal fun searchReady() {
        if (_state.value.searchMode)
            return

        _state.update { it.copy(searchMode = true) }
        searchQuery = ""
    }

    internal fun searchQuery(query: String) {
        if (searchQuery == query)
            return

        searchQuery = query
        searchNodeByQueryString()
    }

    internal fun exitSearch() {
        _state.update { it.copy(searchMode = false) }
        searchQuery = ""
        refreshNodes()
    }

    private fun searchNodeByQueryString() {
        val audios = originalEntities.filter { audio ->
            audio.name.contains(searchQuery, true)
        }
        _state.update {
            it.copy(
                allAudios = audios,
                scrollToTop = true
            )
        }
    }

    internal suspend fun getSelectedNodes(): List<TypedNode> =
        _state.value.selectedAudioHandles.mapNotNull {
            runCatching {
                getNodeByIdUseCase(NodeId(it))
            }.getOrNull()
        }

    internal suspend fun getSelectedMegaNode(): List<MegaNode> =
        _state.value.selectedAudioHandles.mapNotNull {
            runCatching {
                getNodeByHandle(it)
            }.getOrNull()
        }

    internal fun hideOrUnhideNodes(nodeIds: List<NodeId>, hide: Boolean) = viewModelScope.launch {
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
        _state.update {
            it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
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

    internal suspend fun getNodeContentUri(fileNode: TypedFileNode) = runCatching {
        FileNodeContent.AudioOrVideo(uri = getNodeContentUriUseCase(fileNode)).uri
    }.recover {
        Timber.e(it)
        null
    }.getOrNull()

    internal fun getTypedAudioNodeById(id: NodeId) = originalData.firstOrNull { it.id == id }

    internal fun updateClickedItem(value: TypedAudioNode?) =
        _state.update { it.copy(clickedItem = value) }
}