package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetPublicNodeListByIds
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment.Companion.INTENT_KEY_CURRENT_FOLDER_ID
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment.Companion.PARAM_ERROR_MESSAGE
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.util.createDaysCardList
import mega.privacy.android.app.presentation.photos.util.createMonthsCardList
import mega.privacy.android.app.presentation.photos.util.createYearsCardList
import mega.privacy.android.app.presentation.photos.util.groupPhotosByDay
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import org.jetbrains.anko.collections.forEachWithIndex
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MediaDiscoveryViewModel @Inject constructor(
    private val getNodeListByIds: GetNodeListByIds,
    savedStateHandle: SavedStateHandle,
    private val getPhotosByFolderIdUseCase: GetPhotosByFolderIdUseCase,
    private val getCameraSortOrder: GetCameraSortOrder,
    private val setCameraSortOrder: SetCameraSortOrder,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val setMediaDiscoveryView: SetMediaDiscoveryView,
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val authorizeNode: AuthorizeNode,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val getPublicNodeListByIds: GetPublicNodeListByIds,
    private val setViewType: SetViewType,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(
        MediaDiscoveryViewState(
            currentFolderId = savedStateHandle.get<Long>(INTENT_KEY_CURRENT_FOLDER_ID),
            errorMessage = savedStateHandle.get<Int>(PARAM_ERROR_MESSAGE)
        )
    )
    val state = _state.asStateFlow()

    private var fetchPhotosJob: Job? = null
    private var fromFolderLink: Boolean? = null
    internal var showHiddenItems: Boolean? = null

    init {
        fromFolderLink =
            savedStateHandle.get<Boolean>(MediaDiscoveryActivity.INTENT_KEY_FROM_FOLDER_LINK)
        checkConnectivity()
        checkMDSetting()
        loadSortRule()
        monitorPhotos()
        handleHiddenNodes()
    }

    private fun handleHiddenNodes() = viewModelScope.launch {
        if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes) && fromFolderLink != true) {
            monitorShowHiddenItems(
                loadPhotosDone = _state.value.loadPhotosDone,
                sourcePhotos = _state.value.sourcePhotos,
            )
            monitorAccountDetail(
                loadPhotosDone = _state.value.loadPhotosDone,
                sourcePhotos = _state.value.sourcePhotos,
            )
            monitorIsHiddenNodesOnboarded()
        }
    }

    internal fun monitorShowHiddenItems(loadPhotosDone: Boolean, sourcePhotos: List<Photo>) =
        monitorShowHiddenItemsUseCase()
            .onEach {
                showHiddenItems = it
                if (!loadPhotosDone) return@onEach

                handleFolderPhotosAndLogic(sourcePhotos)
            }.launchIn(viewModelScope)

    internal fun monitorAccountDetail(loadPhotosDone: Boolean, sourcePhotos: List<Photo>) =
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                _state.update {
                    it.copy(accountType = accountDetail.levelDetail?.accountType)
                }
                if (!loadPhotosDone) return@onEach

                handleFolderPhotosAndLogic(sourcePhotos)
            }.launchIn(viewModelScope)

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = _state.value.isConnectedToNetwork

    private fun checkConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isConnected ->
                    _state.update {
                        it.copy(isConnectedToNetwork = isConnected)
                    }
                }
        }
    }

    private fun checkMDSetting() {
        viewModelScope.launch {
            monitorMediaDiscoveryView()
                .catch { Timber.e(it) }
                .collectLatest { mediaDiscoveryViewSettings ->
                    _state.update {
                        it.copy(
                            mediaDiscoveryViewSettings = mediaDiscoveryViewSettings
                                ?: MediaDiscoveryViewSettings.INITIAL.ordinal
                        )
                    }
                }
        }
    }

    private fun loadSortRule() {
        viewModelScope.launch {
            val sortOrder = getCameraSortOrder()
            val sort = mapSortOrderToSort(sortOrder)
            _state.update {
                it.copy(
                    currentSort = sort
                )
            }
        }
    }

    private fun saveSortRule(sort: Sort) {
        viewModelScope.launch {
            setCameraSortOrder(mapSortToSortOrder(sort))
        }
    }

    private fun monitorPhotos() {
        fetchPhotosJob?.cancel()

        val currentFolderId = _state.value.currentFolderId

        fetchPhotosJob = currentFolderId?.let { folderId ->
            viewModelScope.launch {
                monitorSubFolderMediaDiscoverySettingsUseCase()
                    .catch { Timber.e(it) }
                    .collectLatest { isRecursive ->
                        getPhotosByFolderId(folderId, isRecursive, fromFolderLink == true)
                    }
            }
        }
    }


    private suspend fun getPhotosByFolderId(
        folderId: Long,
        isRecursive: Boolean,
        isFromFolderLink: Boolean,
    ) {
        getPhotosByFolderIdUseCase(
            folderId = NodeId(folderId),
            recursive = isRecursive,
            isFromFolderLink = isFromFolderLink
        )
            .catch { Timber.e(it) }
            .conflate()
            .collect { sourcePhotos ->
                handleFolderPhotosAndLogic(sourcePhotos)
            }
    }

    internal suspend fun handleFolderPhotosAndLogic(
        sourcePhotos: List<Photo>,
    ) {
        if (sourcePhotos.isEmpty() && isMDFolderInRubbish()) {
            _state.update {
                it.copy(shouldGoBack = true)
            }
        } else {
            handlePhotoItems(
                sortedPhotosWithoutHandleSensitive = sortAndFilterPhotos(sourcePhotos),
                sourcePhotos = sourcePhotos
            )
        }
    }

    private suspend fun isMDFolderInRubbish() =
        _state.value.currentFolderId?.let { currentFolderId ->
            isNodeInRubbishBinUseCase(NodeId(currentFolderId))
        } ?: false

    internal fun sortAndFilterPhotos(sourcePhotos: List<Photo>): List<Photo> {
        val filteredPhotos = when (_state.value.currentMediaType) {
            FilterMediaType.ALL_MEDIA -> sourcePhotos
            FilterMediaType.IMAGES -> sourcePhotos.filterIsInstance<Photo.Image>()
            FilterMediaType.VIDEOS -> sourcePhotos.filterIsInstance<Photo.Video>()
        }
        return when (_state.value.currentSort) {
            Sort.NEWEST -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
            Sort.OLDEST -> filteredPhotos.sortedWith(compareBy<Photo> { it.modificationTime }.thenByDescending { it.id })
            else -> filteredPhotos.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
        }
    }

    internal fun filterNonSensitivePhotos(photos: List<Photo>, isPaid: Boolean?): List<Photo> {
        val showHiddenItems = showHiddenItems ?: return photos
        isPaid ?: return photos

        return if (showHiddenItems || !isPaid) {
            photos
        } else {
            photos.filter { !it.isSensitive && !it.isSensitiveInherited }
        }
    }

    internal fun handlePhotoItems(
        sortedPhotosWithoutHandleSensitive: List<Photo>,
        sourcePhotos: List<Photo>? = null,
    ) = viewModelScope.launch(defaultDispatcher) {
        val sortedPhotos = filterNonSensitivePhotos(
            photos = sortedPhotosWithoutHandleSensitive,
            isPaid = _state.value.accountType?.isPaid,
        )
        val dayPhotos = groupPhotosByDay(sortedPhotos = sortedPhotos)
        val yearsCardList = createYearsCardList(dayPhotos = dayPhotos)
        val monthsCardList = createMonthsCardList(dayPhotos = dayPhotos)
        val daysCardList = createDaysCardList(dayPhotos = dayPhotos)
        val currentZoomLevel = _state.value.currentZoomLevel
        val uiPhotoList = mutableListOf<UIPhoto>()

        sortedPhotos.forEachWithIndex { index, photo ->
            val shouldShowDate = if (index == 0)
                true
            else
                needsDateSeparator(
                    current = photo,
                    previous = sortedPhotos[index - 1],
                    currentZoomLevel = currentZoomLevel
                )
            if (shouldShowDate) {
                uiPhotoList.add(UIPhoto.Separator(photo.modificationTime))
            }
            uiPhotoList.add(UIPhoto.PhotoItem(photo))
        }
        if (sourcePhotos == null) {
            _state.update {
                it.copy(
                    loadPhotosDone = true,
                    uiPhotoList = uiPhotoList,
                    yearsCardList = yearsCardList,
                    monthsCardList = monthsCardList,
                    daysCardList = daysCardList,
                )
            }
        } else {
            _state.update {
                it.copy(
                    loadPhotosDone = true,
                    sourcePhotos = sourcePhotos,
                    uiPhotoList = uiPhotoList,
                    yearsCardList = yearsCardList,
                    monthsCardList = monthsCardList,
                    daysCardList = daysCardList,
                )
            }
        }
    }

    private fun mapSortOrderToSort(sortOrder: SortOrder): Sort = when (sortOrder) {
        SortOrder.ORDER_MODIFICATION_DESC -> Sort.NEWEST
        SortOrder.ORDER_MODIFICATION_ASC -> Sort.OLDEST
        else -> Sort.NEWEST
    }

    private fun mapSortToSortOrder(sort: Sort): SortOrder = when (sort) {
        Sort.NEWEST -> SortOrder.ORDER_MODIFICATION_DESC
        Sort.OLDEST -> SortOrder.ORDER_MODIFICATION_ASC
        else -> SortOrder.ORDER_MODIFICATION_DESC
    }

    private fun needsDateSeparator(
        current: Photo,
        previous: Photo,
        currentZoomLevel: ZoomLevel,
    ): Boolean {
        val currentDate = current.modificationTime.toLocalDate()
        val previousDate = previous.modificationTime.toLocalDate()
        return if (currentZoomLevel == ZoomLevel.Grid_1) {
            currentDate != previousDate
        } else {
            currentDate.month != previousDate.month
        }
    }

    fun togglePhotoSelection(id: Long) {
        val selectedPhotoIds = _state.value.selectedPhotoIds.toMutableSet()
        if (id in selectedPhotoIds) {
            selectedPhotoIds.remove(id)
        } else {
            selectedPhotoIds.add(id)
        }
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
    }

    fun clearSelectedPhotos() {
        _state.update {
            it.copy(selectedPhotoIds = emptySet())
        }
    }

    fun getSelectedIds() = _state.value.selectedPhotoIds.toList()

    fun selectAllPhotos() {
        _state.update {
            it.copy(selectedPhotoIds = getAllPhotoIds().toMutableSet())
        }
    }

    fun getAllPhotoIds() = _state.value.uiPhotoList
        .filterIsInstance<UIPhoto.PhotoItem>()
        .map { (photo) ->
            photo.id
        }

    suspend fun getAllPhotoNodes() = getNodesByIds(getAllPhotoIds())

    suspend fun getNodes() =
        if (_state.value.selectedPhotoIds.isNotEmpty()) {
            getSelectedNodes()
        } else {
            getAllPhotoNodes()
        }


    suspend fun getSelectedNodes(): List<MegaNode> =
        getNodesByIds(_state.value.selectedPhotoIds.toList())

    suspend fun getSelectedTypedNodes(): List<TypedNode> =
        _state.value.selectedPhotoIds.mapNotNull {
            getNodeByIdUseCase(NodeId(it))
        }

    private suspend fun getNodesByIds(ids: List<Long>) = runCatching {
        if (fromFolderLink == true) {
            getPublicNodeListByIds(ids)
        } else {
            getNodeListByIds(ids)
        }
    }.onFailure { Timber.e(it) }.getOrDefault(emptyList())


    fun setCurrentSort(sort: Sort) {
        _state.update {
            it.copy(currentSort = sort)
        }
        if (_state.value.sourcePhotos.isNotEmpty()) {
            handlePhotoItems(sortAndFilterPhotos(_state.value.sourcePhotos))
        }
        saveSortRule(sort)
    }

    fun setCurrentMediaType(mediaType: FilterMediaType) {
        _state.update {
            it.copy(currentMediaType = mediaType)
        }
        if (_state.value.sourcePhotos.isNotEmpty()) {
            handlePhotoItems(sortAndFilterPhotos(_state.value.sourcePhotos))
        }
    }

    fun onTimeBarTabSelected(timeBarTab: TimeBarTab) {
        updateSelectedTimeBarState(selectedTimeBarTab = timeBarTab)
    }

    fun onCardClick(dateCard: DateCard) {
        when (dateCard) {
            is DateCard.YearsCard -> {
                updateSelectedTimeBarState(TimeBarTab.Months,
                    _state.value.monthsCardList.indexOfFirst {
                        it.photo.modificationTime == dateCard.photo.modificationTime
                    })
            }

            is DateCard.MonthsCard -> {
                updateSelectedTimeBarState(TimeBarTab.Days,
                    _state.value.daysCardList.indexOfFirst {
                        it.photo.modificationTime == dateCard.photo.modificationTime
                    })
            }

            is DateCard.DaysCard -> {
                updateSelectedTimeBarState(
                    TimeBarTab.All,
                    _state.value.uiPhotoList.indexOfFirst {
                        it.key == dateCard.photo.id.toString()
                    },
                )
            }
        }
    }

    private fun updateSelectedTimeBarState(
        selectedTimeBarTab: TimeBarTab,
        startIndex: Int = 0,
        startOffset: Int = 0,
    ) {
        _state.update {
            it.copy(
                selectedTimeBarTab = selectedTimeBarTab,
                scrollStartIndex = startIndex,
                scrollStartOffset = startOffset
            )
        }
    }

    fun updateZoomLevel(zoomLevel: ZoomLevel) {
        _state.update {
            it.copy(currentZoomLevel = zoomLevel)
        }
    }

    fun showSortByDialog(showSortByDialog: Boolean) {
        _state.update {
            it.copy(showSortByDialog = showSortByDialog)
        }
    }

    fun showFilterDialog(showFilterDialog: Boolean) {
        _state.update {
            it.copy(showFilterDialog = showFilterDialog)
        }
    }


    fun showSlidersPopup(showSlidersPopup: Boolean) {
        _state.update {
            it.copy(showSlidersPopup = showSlidersPopup)
        }
    }

    /**
     * Set media discovery view is enabled
     */
    fun setMediaDiscoveryViewSettings(mediaDiscoveryViewSettings: Int) {
        viewModelScope.launch {
            setMediaDiscoveryView(mediaDiscoveryViewSettings)
            _state.update {
                it.copy(mediaDiscoveryViewSettings = mediaDiscoveryViewSettings)
            }
        }
    }

    /**
     * Get node parent handle
     *
     * @param handle node handle
     * @return parent handle
     */
    suspend fun getNodeParentHandle(handle: Long): Long? =
        getNodeByHandle(handle)?.parentHandle

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param intent Intent
     * @param isFolderLink true is from folder link, otherwise is false
     * @return updated intent
     */
    suspend fun updateIntent(
        handle: Long,
        name: String,
        intent: Intent,
        isFolderLink: Boolean = false,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        if (isFolderLink) {
            getLocalFolderLinkFromMegaApiUseCase(handle)
        } else {
            getFileUrlByNodeHandleUseCase(handle)
        }?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, typeForName(name).type)
            }
        }
        return intent
    }

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    suspend fun isLocalFile(
        handle: Long,
    ): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = FileUtil.getLocalFile(node)
            File(FileUtil.getDownloadLocation(), node.name).let { file ->
                if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                            || (node.fingerprint == getFingerprintUseCase(localPath)))
                ) {
                    localPath
                } else {
                    null
                }
            }
        }

    /**
     * Checks the list of nodes to copy in order to know which names already exist
     *
     * @param nodes         List of node handles to copy.
     * @param toHandle      Handle of destination node
     */
    fun checkNameCollision(nodes: List<MegaNode>, toHandle: Long) = viewModelScope.launch {
        runCatching {
            checkNameCollisionUseCase.checkNodeListAsync(
                nodes = nodes,
                parentHandle = toHandle,
                type = NameCollisionType.COPY
            )
        }.onSuccess { result ->
            val collisions = result.first
            if (collisions.isNotEmpty()) {
                _state.update {
                    it.copy(collisions = collisions)
                }
            }
            val nodesWithoutCollisions = result.second.associate {
                it.handle to toHandle
            }
            if (nodesWithoutCollisions.isNotEmpty()) {
                runCatching {
                    copyNodesUseCase(nodesWithoutCollisions)
                }.onSuccess { copyResult ->
                    _state.update {
                        it.copy(
                            copyResultText = copyRequestMessageMapper(copyResult.toCopyRequestResult())
                        )
                    }
                }.onFailure { throwable ->
                    _state.update {
                        it.copy(copyThrowable = throwable)
                    }
                }
            }
        }.onFailure { throwable ->
            Timber.e(throwable)
        }
    }

    /**
     * Reset values once show copy result is processed
     */
    fun resetShowCopyResult() {
        _state.update {
            it.copy(copyResultText = null, copyThrowable = null)
        }
    }

    /**
     * Reset values once collision activity is launched
     */
    fun resetLaunchCollisionActivity() {
        _state.update {
            it.copy(collisions = null)
        }
    }

    suspend fun authorizeNodeById(id: Long): MegaNode? = authorizeNode(id)

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val hasCredentials = hasCredentialsUseCase()
            _state.update {
                it.copy(
                    hasDbCredentials = hasCredentials
                )
            }
        }
    }

    suspend fun setListViewTypeClicked() {
        setViewType(ViewType.LIST)
    }

    /**
     * Consume download event once it's started
     */
    fun consumeDownloadEvent() {
        _state.update {
            it.copy(downloadEvent = consumed())
        }
    }

    /**
     * On save to device clicked, will start downloading selected nodes (or all if none selected)
     */
    fun onSaveToDeviceClicked() {
        viewModelScope.launch {
            val nodes = getNodes().mapNotNull {
                if (fromFolderLink == true) {
                    getPublicChildNodeFromIdUseCase(NodeId(it.handle))
                } else {
                    getNodeByIdUseCase(NodeId(it.handle))
                }
            }
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadNode(nodes)))
            }
            clearSelectedPhotos()
        }
    }

    /**
     * Hide or Unhide the selected nodes
     */
    fun hideOrUnhideNodes(hide: Boolean) = viewModelScope.launch {
        for (nodeId in _state.value.selectedPhotoIds) {
            async {
                runCatching {
                    updateNodeSensitiveUseCase(nodeId = NodeId(nodeId), isSensitive = hide)
                }.onFailure { Timber.e("Hide node exception: $it") }
            }
        }
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
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
}
