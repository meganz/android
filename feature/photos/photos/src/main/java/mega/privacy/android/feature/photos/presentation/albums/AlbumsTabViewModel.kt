package mega.privacy.android.feature.photos.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.media.CreateUserAlbumUseCase
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlbumsTabViewModel @Inject constructor(
    private val albumsProvider: Set<@JvmSuppressWildcards AlbumsDataProvider>,
    private val createUserAlbumUseCase: CreateUserAlbumUseCase,
) : ViewModel() {
    internal val uiState: StateFlow<AlbumsTabUiState> by lazy {
        combine(
            flows = albumsProvider
                .sortedBy { it.order }
                .map { it.monitorAlbums() },
            transform = { albums ->
                albums.toList().flatten()
            }
        )
            .map { AlbumsTabUiState(albums = it) }
            .catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                AlbumsTabUiState()
            )
    }

    fun addNewAlbum(name: String) {
        viewModelScope.launch {
            runCatching {
                createUserAlbumUseCase(name)
            }.onFailure {
                Timber.e(it)
            }.onSuccess {
                Timber.d("AlbumsTabViewModel: $name created")
            }
        }
    }
}