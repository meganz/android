package mega.privacy.android.app.presentation.photos.mediadiscovery

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment.Companion.INTENT_KEY_CURRENT_FOLDER_ID
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
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB
import mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.camerauploads.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerSetMaxBufferSizeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import org.jetbrains.anko.collections.forEachWithIndex
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MediaDiscoveryViewModel @Inject constructor(
    private val getNodeListByIds: GetNodeListByIds,
    private val savedStateHandle: SavedStateHandle,
    private val getPhotosByFolderIdUseCase: GetPhotosByFolderIdUseCase,
    private val getCameraSortOrder: GetCameraSortOrder,
    private val setCameraSortOrder: SetCameraSortOrder,
    private val monitorMediaDiscoveryView: MonitorMediaDiscoveryView,
    private val setMediaDiscoveryView: SetMediaDiscoveryView,
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerSetMaxBufferSizeUseCase: MegaApiHttpServerSetMaxBufferSizeUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,

    ) : ViewModel() {

    private val _state = MutableStateFlow(MediaDiscoveryViewState())
    val state = _state.asStateFlow()

    private var fetchPhotosJob: Job? = null

    init {
        checkMDSetting()
        loadSortRule()
        fetchPhotos()
    }

    private fun checkMDSetting() {
        viewModelScope.launch {
            monitorMediaDiscoveryView().collectLatest { mediaDiscoveryViewSettings ->
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

    private fun fetchPhotos() {
        fetchPhotosJob?.cancel()

        val currentFolderId = savedStateHandle.get<Long>(INTENT_KEY_CURRENT_FOLDER_ID)
        fetchPhotosJob = currentFolderId?.let {
            viewModelScope.launch {
                getPhotosByFolderIdUseCase(folderId = NodeId(it), recursive = true)
                    .collectLatest { sourcePhotos ->
                        handlePhotoItems(
                            sortedPhotos = sortAndFilterPhotos(sourcePhotos),
                            sourcePhotos = sourcePhotos
                        )
                    }
            }
        }
    }

    private fun sortAndFilterPhotos(sourcePhotos: List<Photo>): List<Photo> {
        val filteredPhotos = when (_state.value.currentMediaType) {
            FilterMediaType.ALL_MEDIA -> sourcePhotos
            FilterMediaType.IMAGES -> sourcePhotos.filterIsInstance<Photo.Image>()
            FilterMediaType.VIDEOS -> sourcePhotos.filterIsInstance<Photo.Video>()
        }
        return when (_state.value.currentSort) {
            Sort.NEWEST -> filteredPhotos.sortedByDescending { it.modificationTime }
            Sort.OLDEST -> filteredPhotos.sortedBy { it.modificationTime }
            else -> filteredPhotos.sortedByDescending { it.modificationTime }
        }
    }

    private fun handlePhotoItems(sortedPhotos: List<Photo>, sourcePhotos: List<Photo>? = null) {
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
                    uiPhotoList = uiPhotoList,
                    shouldBack = shouldBack(uiPhotoList),
                    yearsCardList = yearsCardList,
                    monthsCardList = monthsCardList,
                    daysCardList = daysCardList,
                )
            }
        } else {
            _state.update {
                it.copy(
                    sourcePhotos = sourcePhotos,
                    uiPhotoList = uiPhotoList,
                    shouldBack = shouldBack(uiPhotoList),
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

    private fun shouldBack(uiPhotoList: MutableList<UIPhoto>) =
        _state.value.currentMediaType == FilterMediaType.ALL_MEDIA
                && uiPhotoList.isEmpty()

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

    suspend fun getSelectedNodes() =
        getNodeListByIds(_state.value.selectedPhotoIds.toList())

    fun setCurrentSort(sort: Sort) {
        _state.update {
            it.copy(currentSort = sort)
        }
        handlePhotoItems(sortAndFilterPhotos(_state.value.sourcePhotos))
        saveSortRule(sort)
    }

    fun setCurrentMediaType(mediaType: FilterMediaType) {
        _state.update {
            it.copy(currentMediaType = mediaType)
        }
        handlePhotoItems(sortAndFilterPhotos(_state.value.sourcePhotos))
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
     * @param isNeedsMoreBufferSize true is that sets 32MB, otherwise is false
     * @param intent Intent
     * @return updated intent
     */
    suspend fun updateIntent(
        handle: Long,
        name: String,
        isNeedsMoreBufferSize: Boolean,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        megaApiHttpServerSetMaxBufferSizeUseCase(
            if (isNeedsMoreBufferSize) {
                MAX_BUFFER_32MB
            } else {
                MAX_BUFFER_16MB
            }
        )

        getFileUrlByNodeHandleUseCase(handle)?.let { url ->
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
}
