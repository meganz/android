package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsLoadState
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.domain.usecase.GetAlbums
import timber.log.Timber
import javax.inject.Inject

/**
 * AlbumsViewModel handle albums cover page view logic
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getAlbums: GetAlbums,
) : ViewModel() {

    private val _loadState =
        MutableStateFlow<AlbumsLoadState>(AlbumsLoadState.Empty)
    val loadState = _loadState.asStateFlow()
    private val _state = MutableStateFlow(AlbumsViewState())
    val state = _state.asStateFlow()
    private var currentNodeJob: Job? = null

    init {
        getAlbumCover()
    }

    /**
     * Get all favourites
     */
    private fun getAlbumCover() {
        _loadState.update {
            AlbumsLoadState.Loading
        }
        currentNodeJob = viewModelScope.launch {
            runCatching {
                getAlbums().collectLatest { albums ->
                    _state.value = AlbumsViewState(albums = albums)
                    _loadState.update {
                        AlbumsLoadState.Success(albums)
                    }
                }
            }.onFailure { exception ->
                if (exception is MegaException) {
                    Timber.e(exception)
                }
                _loadState.update {
                    AlbumsLoadState.Empty
                }
            }
        }
    }
}