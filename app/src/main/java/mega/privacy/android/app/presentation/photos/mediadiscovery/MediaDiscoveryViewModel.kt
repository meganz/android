package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetPublicNodeListByIds
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.MediaListItem
import mega.privacy.android.app.presentation.photos.model.MediaListItem.PhotoItem
import mega.privacy.android.app.presentation.photos.model.MediaListMedia
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.util.createDaysCardList
import mega.privacy.android.app.presentation.photos.util.createMonthsCardList
import mega.privacy.android.app.presentation.photos.util.createYearsCardList
import mega.privacy.android.app.presentation.photos.util.groupPhotosByDay
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.components.banners.StorageCapacityMapper
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.MonitorAlmostFullStorageBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetAlmostFullStorageBannerClosingTimestampUseCase
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.navigation.ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for MediaDiscoveryFragment
 */
@HiltViewModel
class MediaDiscoveryViewModel @Inject constructor(
    private val getNodeListByIds: GetNodeListByIds,
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
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase,
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
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val storageCapacityMapper: StorageCapacityMapper,
    private val setAlmostFullStorageBannerClosingTimestampUseCase: SetAlmostFullStorageBannerClosingTimestampUseCase,
    private val monitorAlmostFullStorageBannerClosingTimestampUseCase: MonitorAlmostFullStorageBannerVisibilityUseCase,
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDiscoveryViewState())
    val state = _state.asStateFlow()

    private var fetchPhotosJob: Job? = null
    private var fromFolderLink: Boolean? = null
    internal var showHiddenItems: Boolean? = null

    internal fun initialize(folderId: Long?, errorMessage: Int?, fromFolderLink: Boolean?) {
        _state.update {
            it.copy(
                currentFolderId = folderId,
                errorMessage = errorMessage.takeIf { errorMessage -> errorMessage != 0 }
            )
        }
        this.fromFolderLink = fromFolderLink
        checkConnectivity()
        checkMDSetting()
        loadSortRule()
        monitorPhotos()
        handleHiddenNodes()
        monitorStorageOverQuotaCapacity()
    }

    private fun monitorStorageOverQuotaCapacity() {
        viewModelScope.launch {
            combine(
                monitorStorageStateUseCase(),
                monitorAlmostFullStorageBannerClosingTimestampUseCase()
            )
            { storageState, shouldShow ->
                storageCapacityMapper(
                    storageState = storageState,
                    shouldShow = shouldShow
                )
            }.catch { Timber.e(it) }
                .collectLatest { storageCapacity ->
                    _state.update {
                        it.copy(storageCapacity = storageCapacity)
                    }
                }
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() == true
    }

    private fun handleHiddenNodes() = viewModelScope.launch {
        if (isHiddenNodesActive() && fromFolderLink != true) {
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
                val accountType = accountDetail.levelDetail?.accountType
                val businessStatus =
                    if (accountType?.isBusinessAccount == true) {
                        getBusinessStatusUseCase()
                    } else null

                _state.update {
                    it.copy(
                        accountType = accountType,
                        isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                        hiddenNodeEnabled = true,
                    )
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

        return if (showHiddenItems || !isPaid || _state.value.isBusinessAccountExpired) {
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
        val mediaListItemList = mutableListOf<MediaListItem>()

        sortedPhotos.forEachIndexed { index, photo ->
            val shouldShowDate = if (index == 0)
                true
            else
                needsDateSeparator(
                    current = photo,
                    previous = sortedPhotos[index - 1],
                    currentZoomLevel = currentZoomLevel
                )
            if (shouldShowDate) {
                mediaListItemList.add(MediaListItem.Separator(photo.modificationTime))
            }
            val item = when (photo) {
                is Photo.Image -> PhotoItem(photo)
                is Photo.Video -> MediaListItem.VideoItem(
                    photo,
                    durationInSecondsTextMapper(photo.fileTypeInfo.duration)
                )
            }
            mediaListItemList.add(item)
        }
        if (sourcePhotos == null) {
            _state.update {
                it.copy(
                    loadPhotosDone = true,
                    mediaListItemList = mediaListItemList,
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
                    mediaListItemList = mediaListItemList,
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
            currentDate.month != previousDate.month || currentDate.year != previousDate.year
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

    internal fun updateSelectedPhotoIds(photoIds: Set<Long>) {
        _state.update {
            it.copy(selectedPhotoIds = photoIds)
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

    fun getAllPhotoIds() = _state.value.mediaListItemList
        .filterIsInstance<MediaListMedia>()
        .map { it.mediaId }

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
                updateSelectedTimeBarState(
                    TimeBarTab.Months,
                    _state.value.monthsCardList.indexOfFirst {
                        it.photo.modificationTime == dateCard.photo.modificationTime
                    })
            }

            is DateCard.MonthsCard -> {
                updateSelectedTimeBarState(
                    TimeBarTab.Days,
                    _state.value.daysCardList.indexOfFirst {
                        it.photo.modificationTime == dateCard.photo.modificationTime
                    })
            }

            is DateCard.DaysCard -> {
                updateSelectedTimeBarState(
                    TimeBarTab.All,
                    _state.value.mediaListItemList.indexOfFirst {
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
     * @param nodeHandles         List of node handles to copy.
     * @param toHandle      Handle of destination node
     */
    fun checkNameCollision(nodeHandles: List<Long>, toHandle: Long) = viewModelScope.launch {
        runCatching {
            checkNodesNameCollisionWithActionUseCase(
                nodes = nodeHandles.associateWith { toHandle },
                type = NodeNameCollisionType.COPY
            )
        }.onSuccess { result ->
            _state.update { state ->
                state.copy(
                    collisions = result.collisionResult.conflictNodes.values.toList(),
                    copyResultText = result.moveRequestResult?.let {
                        copyRequestMessageMapper(it.toCopyRequestResult())
                    }
                )
            }
        }.onFailure { throwable ->
            _state.update {
                it.copy(copyThrowable = throwable)
            }
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
            it.copy(collisions = emptyList())
        }
    }

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
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = nodes,
                            withStartMessage = true,
                        )
                    )
                )
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

    internal suspend fun getNodeContentUri(nodeHandle: Long) =
        getNodeContentUriByHandleUseCase(nodeHandle)

    /**
     * Reset storage capacity to default
     */
    fun setStorageCapacityAsDefault() {
        _state.update { it.copy(storageCapacity = StorageOverQuotaCapacity.DEFAULT) }
        viewModelScope.launch {
            runCatching {
                setAlmostFullStorageBannerClosingTimestampUseCase()
            }.onFailure { Timber.e(it) }
        }
    }

    internal fun updateIsClearSelectedPhotos(isClear: Boolean) {
        _state.update {
            it.copy(isClearSelectedPhotos = isClear)
        }
    }

    internal fun clearState() {
        _state.update { MediaDiscoveryViewState() }
    }
}
