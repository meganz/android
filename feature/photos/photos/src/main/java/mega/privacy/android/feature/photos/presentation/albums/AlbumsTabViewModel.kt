package mega.privacy.android.feature.photos.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlbumsTabViewModel @Inject constructor(
    private val albumsProvider: Set<@JvmSuppressWildcards AlbumsDataProvider>,
) : ViewModel() {
    internal val uiState: StateFlow<AlbumsTabUiState> by lazy {
        combine(
            albumsProvider
                .toList()
                .sortedBy { it.order }
                .map { it.monitorAlbums() }
        ) { albums ->
            albums.toList().flatten()
        }
            .map { AlbumsTabUiState(albums = it) }
            .catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                AlbumsTabUiState()
            )
    }
}