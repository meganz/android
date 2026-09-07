package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MediaMainViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
) : ViewModel() {
    internal val uiState: StateFlow<MediaMainUiState>
        field: MutableStateFlow<MediaMainUiState> = MutableStateFlow(MediaMainUiState())

    init {
        getTimelineRevampFlag()
        getPinchToZoomFlag()
    }

    /**
     * Prefetches the tapped photo's thumbnail while still on the grid, so the viewer can paint it
     * immediately instead of opening blank. The thumbnail is tiny, so it typically lands on disk
     * before the viewer's first frame. The preview/full are left to the viewer's own pipeline, which
     * downloads them on open regardless.
     */
    fun prefetchThumbnail(nodeId: Long) {
        viewModelScope.launch {
            runCatching { downloadThumbnailUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to prefetch thumbnail for $nodeId") }
        }
    }

    private fun getTimelineRevampFlag() {
        viewModelScope.launch {
            runCatching {
                val isEnabled = getFeatureFlagValueUseCase(ApiFeatures.MediaTimelinePagination)
                uiState.update {
                    it.copy(isTimelineRevampEnabled = isEnabled)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getPinchToZoomFlag() {
        viewModelScope.launch {
            runCatching {
                val isEnabled = getFeatureFlagValueUseCase(ApiFeatures.TimelinePinchToZoom)
                uiState.update {
                    it.copy(isTimelinePinchToZoomEnabled = isEnabled)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}