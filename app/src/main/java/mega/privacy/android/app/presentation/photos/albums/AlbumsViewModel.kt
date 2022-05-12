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
import mega.privacy.android.app.domain.usecase.GetAlbums
import mega.privacy.android.app.presentation.photos.model.AlbumsLoadState
import mega.privacy.android.app.usecase.MegaException
import timber.log.Timber
import javax.inject.Inject

/**
 * AlbumsViewModel handle albums cover page view logic
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
        private val getAlbums: GetAlbums
) : ViewModel() {

    private val _favouritesState =
            MutableStateFlow<AlbumsLoadState>(AlbumsLoadState.Empty)
    val favouritesState = _favouritesState.asStateFlow()

    init {
        getFavouriteAlbumCover()
    }

    private var currentNodeJob: Job? = null

    /**
     * Get all favourites
     */
    private fun getFavouriteAlbumCover() {
        _favouritesState.update {
            AlbumsLoadState.Loading
        }
        currentNodeJob = viewModelScope.launch {
            runCatching {
                getAlbums()
            }.onSuccess {
                it.collectLatest { albums ->
                    _favouritesState.update {
                        AlbumsLoadState.Success(albums)
                    }
                }
            }.onFailure { exception ->
                if (exception is MegaException) {
                    Timber.e(exception)
                }
            }
        }
    }
}