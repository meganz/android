package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import javax.inject.Inject

@Deprecated(message = "In favor of mega.privacy.android.app.presentation.photos.albums.albumcontent.AlbumContentViewModel")
@HiltViewModel
class AlbumContentViewModel @Inject constructor(
    private val observeAlbumPhotosAddingProgress: ObserveAlbumPhotosAddingProgress,
    private val updateAlbumPhotosAddingProgressCompleted: UpdateAlbumPhotosAddingProgressCompleted,
    private val observeAlbumPhotosRemovingProgress: ObserveAlbumPhotosRemovingProgress,
    private val updateAlbumPhotosRemovingProgressCompleted: UpdateAlbumPhotosRemovingProgressCompleted,
    private val disableExportAlbumsUseCase: DisableExportAlbumsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumContentState())
    val state = _state.asStateFlow()

    fun setIsLoadingCompleted() {
        _state.update {
            it.copy(isLoadingPhotos = false)
        }
    }

    fun observePhotosRemovingProgress(albumId: AlbumId) {
        observeAlbumPhotosRemovingProgress(albumId)
            .onEach(::handlePhotosRemovingProgress)
            .launchIn(viewModelScope)
    }

    private fun handlePhotosRemovingProgress(progress: AlbumPhotosRemovingProgress?) {
        _state.update {
            it.copy(
                isRemovingPhotos = progress?.isProgressing ?: false,
                totalRemovedPhotos = progress?.totalRemovedPhotos ?: 0,
            )
        }
    }

    fun updatePhotosRemovingProgressCompleted(albumId: AlbumId) = viewModelScope.launch {
        updateAlbumPhotosRemovingProgressCompleted(albumId)
    }

    fun observePhotosAddingProgress(albumId: AlbumId) {
        observeAlbumPhotosAddingProgress(albumId)
            .onEach(::handlePhotosAddingProgress)
            .launchIn(viewModelScope)
    }

    private fun handlePhotosAddingProgress(progress: AlbumPhotosAddingProgress?) {
        _state.update {
            it.copy(
                isAddingPhotos = progress?.isProgressing ?: false,
                totalAddedPhotos = progress?.totalAddedPhotos ?: 0,
            )
        }
    }

    fun updatePhotosAddingProgressCompleted(albumId: AlbumId) = viewModelScope.launch {
        updateAlbumPhotosAddingProgressCompleted(albumId)
    }

    fun deleteAlbum() {
        _state.update {
            it.copy(isDeleteAlbum = true)
        }
    }

    fun showRemoveLinkConfirmation() {
        _state.update {
            it.copy(showRemoveLinkConfirmation = true)
        }
    }

    fun closeRemoveLinkConfirmation() {
        _state.update {
            it.copy(showRemoveLinkConfirmation = false)
        }
    }

    fun disableExportAlbum(albumId: AlbumId) = viewModelScope.launch {
        val numRemoved = disableExportAlbumsUseCase(albumIds = listOf(albumId))
        _state.update {
            it.copy(isLinkRemoved = numRemoved > 0)
        }
    }

    fun resetLinkRemoved() {
        _state.update {
            it.copy(isLinkRemoved = false)
        }
    }
}
