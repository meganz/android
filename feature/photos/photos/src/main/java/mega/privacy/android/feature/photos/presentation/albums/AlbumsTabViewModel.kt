package mega.privacy.android.feature.photos.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlbumsTabViewModel @Inject constructor(
    private val albumsProvider: Set<@JvmSuppressWildcards AlbumsDataProvider>,
    private val validateAndCreateUserAlbumUseCase: ValidateAndCreateUserAlbumUseCase,
    private val albumUiStateMapper: AlbumUiStateMapper,
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper,
) : ViewModel() {
    internal val uiState: StateFlow<AlbumsTabUiState>
        field = MutableStateFlow(AlbumsTabUiState())

    init {
        monitorAlbums()
    }

    private fun monitorAlbums() {
        combine(
            flows = albumsProvider
                .sortedBy { it.order }
                .map { it.monitorAlbums() },
            transform = { albums ->
                albums.toList().flatten()
            }
        ).map { albums ->
            val albumsUiState = albums.map { albumUiStateMapper(it) }
            uiState.update { it.copy(albums = albumsUiState) }
        }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    internal fun addNewAlbum(name: String) {
        viewModelScope.launch {
            runCatching {
                validateAndCreateUserAlbumUseCase(name)
            }.onFailure { e ->
                Timber.e(e)
                if (e is AlbumNameValidationException) {
                    val message = albumNameValidationExceptionMessageMapper(e)
                    uiState.update {
                        it.copy(addNewAlbumErrorMessage = triggered(message))
                    }
                }
            }.onSuccess {
                Timber.d("AlbumsTabViewModel: $name created")
                uiState.update {
                    it.copy(
                        addNewAlbumErrorMessage = consumed(),
                        addNewAlbumSuccessEvent = triggered
                    )
                }
            }
        }
    }

    internal fun resetErrorMessage() {
        uiState.update { it.copy(addNewAlbumErrorMessage = consumed()) }
    }

    internal fun resetAddNewAlbumSuccess() {
        uiState.update { it.copy(addNewAlbumSuccessEvent = consumed) }
    }
}