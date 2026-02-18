package mega.privacy.android.feature.photos.presentation.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState

@Composable
internal fun MediaMainEffects(
    timelineTabUiState: TimelineTabUiState,
    timelineFilterUiState: TimelineFilterUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    checkCameraUploadsPermissions: () -> Unit,
    checkNotificationPermission: () -> Unit,
    updateCUPageEnablementBasedOnDisplayedPhotos: suspend (photos: List<PhotosNodeContentItem>) -> Unit,
    updateSortActionEnablement: (isEnableCameraUploadPageShowing: Boolean, mediaSource: FilterMediaSource) -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        checkCameraUploadsPermissions()
        onPauseOrDispose {}
    }

    LifecycleResumeEffect(Unit) {
        checkNotificationPermission()
        onPauseOrDispose {}
    }

    LaunchedEffect(
        timelineTabUiState.displayedPhotos,
        timelineTabUiState.isLoading,
    ) {
        if (!timelineTabUiState.isLoading) {
            updateCUPageEnablementBasedOnDisplayedPhotos(timelineTabUiState.displayedPhotos)
        }
    }

    LaunchedEffect(
        mediaCameraUploadUiState.enableCameraUploadPageShowing,
        timelineFilterUiState.mediaSource,
        timelineTabUiState.isLoading,
    ) {
        if (!timelineTabUiState.isLoading) {
            updateSortActionEnablement(
                mediaCameraUploadUiState.enableCameraUploadPageShowing,
                timelineFilterUiState.mediaSource
            )
        }
    }
}
