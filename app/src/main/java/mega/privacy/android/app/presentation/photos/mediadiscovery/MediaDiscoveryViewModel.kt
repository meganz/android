package mega.privacy.android.app.presentation.photos.mediadiscovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment.Companion.INTENT_KEY_CURRENT_FOLDER_ID
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetPhotosByFolderId
import org.jetbrains.anko.collections.forEachWithIndex
import javax.inject.Inject

class MediaDiscoveryViewModel @Inject constructor(
    private val getNodeListByIds: GetNodeListByIds,
    savedStateHandle: SavedStateHandle,
    private val getPhotosByFolderId: GetPhotosByFolderId,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDiscoveryViewState())
    val state = _state.asStateFlow()

    internal val selectedPhotoIds = mutableSetOf<Long>()

    init {
        val currentFolderId = savedStateHandle.get<Long>(INTENT_KEY_CURRENT_FOLDER_ID)
        currentFolderId?.let {
            viewModelScope.launch {
                getPhotosByFolderId(currentFolderId)
                    .map {
                        handlePhotoItems(it)
                    }.collectLatest { uiPhotoList ->
                        _state.update {
                            it.copy(
                                uiPhotoList = uiPhotoList
                            )
                        }
                    }
            }
        }
    }

    private fun handlePhotoItems(photos: List<Photo>): List<UIPhoto> {
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
        return uiPhotoList
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
        if (id in selectedPhotoIds) {
            selectedPhotoIds.remove(id)
        } else {
            selectedPhotoIds.add(id)
        }
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
        }
    }

    fun clearSelectedPhotos() {
        selectedPhotoIds.clear()
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
        }
    }

    fun getSelectedIds() = _state.value.selectedPhotoIds.toList()

    fun selectAllPhotos() {
        val allIds =
            _state.value.uiPhotoList.filterIsInstance<UIPhoto.PhotoItem>().map { (photo) ->
                photo.id
            }
        selectedPhotoIds.clear()
        selectedPhotoIds.addAll(allIds)
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
        }
    }

    suspend fun getSelectedNodes() =
        getNodeListByIds(selectedPhotoIds.toList())

    fun setCurrentSort(sort: Sort) {
        _state.update {
            it.copy(currentSort = sort)
        }
    }
}