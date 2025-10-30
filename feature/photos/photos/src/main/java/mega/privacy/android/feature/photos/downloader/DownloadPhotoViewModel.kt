package mega.privacy.android.feature.photos.downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.photos.DownloadPhotoRequest
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.DownloadPhotoUseCase
import javax.inject.Inject

/**
 * A viewmodel for downloading photos.
 *
 * @property downloadPhotoUseCase [DownloadPhotoUseCase].
 */
@HiltViewModel
class DownloadPhotoViewModel @Inject constructor(
    private val downloadPhotoUseCase: DownloadPhotoUseCase,
) : ViewModel() {

    private val downloadResultMap =
        object : LinkedHashMap<Long, StateFlow<DownloadPhotoResult>>(MAX_DOWNLOAD_CACHE_SIZE) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Long, StateFlow<DownloadPhotoResult>>?,
            ): Boolean = size > MAX_DOWNLOAD_CACHE_SIZE
        }
    private val cacheMutex = Mutex()

    /**
     * Get [DownloadPhotoResult] as a [StateFlow].
     *
     * @param photo [Photo].
     * @param isPreview whether the download is for preview. The value is true if the photo
     * screen is in portrait mode and zoomed in so that only 1 photo item is displayed per row.
     * @param isPublicNode whether the download request is from the MediaDiscoveryScreen
     *
     * @return a [StateFlow] of [DownloadPhotoResult] for the specified photo ID. A new StateFlow
     * is returned for each photo ID. If a StateFlow for a photo ID already exists, the previously
     * created instance will be returned.
     */
    suspend fun getDownloadPhotoResult(
        photo: Photo,
        isPreview: Boolean,
        isPublicNode: Boolean = false,
    ): StateFlow<DownloadPhotoResult> = cacheMutex.withLock {
        downloadResultMap.getOrPut(photo.id) {
            flow {
                val result = runCatching {
                    downloadPhotoUseCase(
                        request = DownloadPhotoRequest(
                            isPreview = isPreview,
                            photo = photo,
                            isPublicNode = isPublicNode
                        )
                    )
                }.getOrDefault(defaultValue = DownloadPhotoResult.Error)
                emit(result)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = DownloadPhotoResult.Idle
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            cacheMutex.withLock {
                downloadResultMap.clear()
            }
        }
    }

    companion object {
        private const val MAX_DOWNLOAD_CACHE_SIZE = 300
    }
}
