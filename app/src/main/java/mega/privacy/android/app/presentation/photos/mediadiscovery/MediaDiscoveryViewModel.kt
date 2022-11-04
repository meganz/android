package mega.privacy.android.app.presentation.photos.mediadiscovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment.Companion.INTENT_KEY_CURRENT_FOLDER_ID
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.util.createDaysCardList
import mega.privacy.android.app.presentation.photos.util.createMonthsCardList
import mega.privacy.android.app.presentation.photos.util.createYearsCardList
import mega.privacy.android.app.presentation.photos.util.groupPhotosByDay
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetPhotosByFolderId
import org.jetbrains.anko.collections.forEachWithIndex
import javax.inject.Inject

@HiltViewModel
class MediaDiscoveryViewModel @Inject constructor(
    private val getNodeListByIds: GetNodeListByIds,
    savedStateHandle: SavedStateHandle,
    private val getPhotosByFolderId: GetPhotosByFolderId,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDiscoveryViewState())
    val state = _state.asStateFlow()

    init {
        val currentFolderId = savedStateHandle.get<Long>(INTENT_KEY_CURRENT_FOLDER_ID)
        currentFolderId?.let {
            viewModelScope.launch {
                getPhotosByFolderId(currentFolderId)
                    .collectLatest { sourcePhotos ->
                        handlePhotoItems(sourcePhotos)
                    }
            }
        }
    }

    private fun handlePhotoItems(sourcePhotos: List<Photo>, sort: Sort = _state.value.currentSort) {
        val photos = sortPhotos(sourcePhotos, sort)
        val dayPhotos = groupPhotosByDay(photos, sort)
        val yearsCardList = createYearsCardList(dayPhotos)
        val monthsCardList = createMonthsCardList(dayPhotos)
        val daysCardList = createDaysCardList(dayPhotos)
        val currentZoomLevel = _state.value.currentZoomLevel
        val uiPhotoList = mutableListOf<UIPhoto>()

        photos.forEachWithIndex { index, photo ->
            val shouldShowDate = if (index == 0)
                true
            else
                needsDateSeparator(
                    current = photo,
                    previous = photos[index - 1],
                    currentZoomLevel = currentZoomLevel
                )
            if (shouldShowDate) {
                uiPhotoList.add(UIPhoto.Separator(photo.modificationTime))
            }
            uiPhotoList.add(UIPhoto.PhotoItem(photo))
        }
        _state.update {
            it.copy(
                uiPhotoList = uiPhotoList,
                yearsCardList = yearsCardList,
                monthsCardList = monthsCardList,
                daysCardList = daysCardList,
                currentSort = sort
            )
        }
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

    private fun sortPhotos(
        photos: List<Photo>,
        sort: Sort = _state.value.currentSort,
    ): List<Photo> = when (sort) {
        Sort.NEWEST -> photos.sortedByDescending { it.modificationTime }
        Sort.OLDEST -> photos.sortedBy { it.modificationTime }
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

    private fun getAllPhotos() = _state.value.uiPhotoList.filterIsInstance<UIPhoto.PhotoItem>()
        .map { (photo) ->
            photo
        }

    fun getAllPhotoIds() = _state.value.uiPhotoList
        .filterIsInstance<UIPhoto.PhotoItem>()
        .map { (photo) ->
            photo.id
        }

    suspend fun getSelectedNodes() =
        getNodeListByIds(_state.value.selectedPhotoIds.toList())

    fun setCurrentSort(sort: Sort) {
        handlePhotoItems(
            sourcePhotos = getAllPhotos(),
            sort = sort
        )
    }

    fun onTimeBarTabSelected(timeBarTab: TimeBarTab) {
        _state.update {
            it.copy(selectedTimeBarTab = timeBarTab)
        }
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
                    }
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

    fun zoomIn() {
        if (_state.value.currentZoomLevel == ZoomLevel.values().first()
        ) {
            return
        }
        _state.update {
            it.copy(currentZoomLevel = ZoomLevel.values()[_state.value.currentZoomLevel.ordinal - 1])
        }
    }

    fun zoomOut() {
        if (_state.value.currentZoomLevel == ZoomLevel.values().last()
        ) {
            return
        }

        _state.update {
            it.copy(currentZoomLevel = ZoomLevel.values()[_state.value.currentZoomLevel.ordinal + 1])
        }
    }
}