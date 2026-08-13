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
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPreviewUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MediaMainViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val downloadPreviewUseCase: DownloadPreviewUseCase,
) : ViewModel() {
    internal val uiState: StateFlow<MediaMainUiState>
        field: MutableStateFlow<MediaMainUiState> = MutableStateFlow(MediaMainUiState())

    init {
        getTimelineRevampFlag()
    }

    /**
     * Prefetches the tapped photo's preview into the cache while the viewer opens, so it can show the
     * preview immediately instead of downloading it on open (the grid only caches thumbnails).
     */
    fun prefetchPreview(nodeId: Long) {
        viewModelScope.launch {
            runCatching { downloadPreviewUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to prefetch preview for $nodeId") }
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
}