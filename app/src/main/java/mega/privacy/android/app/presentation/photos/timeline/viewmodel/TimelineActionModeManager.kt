package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.photos.Photo




internal fun TimelineViewModel.clearSelectedPhotos() {
    selectedPhotosIds.clear()
    _state.update {
        it.copy(
            photosListItems = setSelectedPhotos(it.photosListItems),
            selectedPhotoCount = selectedPhotosIds.size,
        )
    }
}

internal fun TimelineViewModel.selectAllShowingPhotos() {
    selectedPhotosIds.addAll(
        _state.value.currentShowingPhotos.map {
            it.id
        }
    )
    _state.update {
        it.copy(
            photosListItems = setSelectedPhotos(it.photosListItems),
            selectedPhotoCount = selectedPhotosIds.size,
        )
    }
}