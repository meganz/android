package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource

fun TimelineViewModel.zoomIn() {
    if (_state.value.currentZoomLevel == ZoomLevel.values()[ZoomLevel.values().first().ordinal]) {
        return
    }
    _state.update {
        it.copy(currentZoomLevel = ZoomLevel.values()[_state.value.currentZoomLevel.ordinal - 1])
    }
    handleEnableZoomAndSortOptions()
}

fun TimelineViewModel.zoomOut() {
    if (_state.value.currentZoomLevel == ZoomLevel.values()[ZoomLevel.values().last().ordinal]) {
        return
    }

    _state.update {
        it.copy(currentZoomLevel = ZoomLevel.values()[_state.value.currentZoomLevel.ordinal + 1])
    }
    handleEnableZoomAndSortOptions()
}

fun TimelineViewModel.handleEnableZoomAndSortOptions() {
    if (_state.value.currentShowingPhotos.isEmpty()
        || (_state.value.enableCameraUploadPageShowing
                && _state.value.currentMediaSource != TimelinePhotosSource.CLOUD_DRIVE)
    ) {
        _state.update {
            it.copy(
                enableZoomIn = false,
                enableZoomOut = false,
                enableSortOption = false,
            )
        }
    } else {
        when (_state.value.currentZoomLevel) {
            ZoomLevel.values()[ZoomLevel.values().first().ordinal] -> {
                _state.update {
                    it.copy(
                        enableZoomIn = false,
                        enableZoomOut = true,
                        enableSortOption = true,
                    )
                }
            }

            ZoomLevel.values()[ZoomLevel.values().last().ordinal] -> {
                _state.update {
                    it.copy(
                        enableZoomIn = true,
                        enableZoomOut = false,
                        enableSortOption = true,
                    )
                }
            }

            else -> {
                _state.update {
                    it.copy(
                        enableZoomIn = true,
                        enableZoomOut = true,
                        enableSortOption = true
                    )
                }
            }
        }
    }

}

fun TimelineViewModel.getCurrentSort(): Sort = _state.value.currentSort

fun TimelineViewModel.getFilterType(): FilterMediaType = _state.value.currentFilterMediaType

fun TimelineViewModel.getMediaSource(): TimelinePhotosSource = _state.value.currentMediaSource

fun TimelineViewModel.setCurrentSort(sort: Sort) {
    _state.update {
        it.copy(currentSort = sort)
    }
}

fun TimelineViewModel.showingSortByDialog(isShowing: Boolean) {
    _state.update {
        it.copy(showingSortByDialog = isShowing)
    }
}

fun TimelineViewModel.showingFilterPage(isShowing: Boolean) {
    _state.update {
        it.copy(showingFilterPage = isShowing)
    }
}

fun TimelineViewModel.shouldEnableCUPage(show: Boolean) {
    if (show && _state.value.currentMediaSource != TimelinePhotosSource.CLOUD_DRIVE) {
        _state.update {
            it.copy(
                enableCameraUploadPageShowing = true,
                enableZoomIn = false,
                enableZoomOut = false,
                enableSortOption = false,
            )
        }
    } else {
        _state.update {
            it.copy(enableCameraUploadPageShowing = false)
        }
        handleEnableZoomAndSortOptions()
    }
}

fun TimelineViewModel.setShowProgressBar(show: Boolean) {
    _state.update {
        it.copy(progressBarShowing = show)
    }
}

fun TimelineViewModel.setProgress(progress: Float) {
    _state.update {
        it.copy(progress = progress)
    }
}

fun TimelineViewModel.updateProgress(
    pending: Int = 0,
    showProgress: Boolean = false,
    progress: Float = 0f,
) {
    _state.update {
        it.copy(pending = pending, progressBarShowing = showProgress, progress = progress)
    }
}