package mega.privacy.android.feature.photos.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.usecase.GetUserAlbums
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlbumsTabViewModel @Inject constructor(
    private val getUserAlbums: GetUserAlbums,
) : ViewModel() {
    internal val uiState: StateFlow<AlbumsTabUiState>
        field = MutableStateFlow(AlbumsTabUiState())

    init {
        monitorAlbums()
    }

    private fun monitorAlbums() {
        getUserAlbums()
            .catch { Timber.e(it) }
            .onEach { albums ->
                uiState.update { it.copy(albums = albums) }
                Timber.d("Albums: ${albums.joinToString { it.title }}")
            }
            .launchIn(viewModelScope)
    }
}