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
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumSelectionAction
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumGetMultipleLinksNavKey
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlbumsTabViewModel @Inject constructor(
    private val albumsProvider: Set<@JvmSuppressWildcards AlbumsDataProvider>,
    private val validateAndCreateUserAlbumUseCase: ValidateAndCreateUserAlbumUseCase,
    private val albumUiStateMapper: AlbumUiStateMapper,
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper,
    private val removeAlbumsUseCase: RemoveAlbumsUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
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
            }.onSuccess { albumId ->
                Timber.d("AlbumsTabViewModel: $name created")
                uiState.update {
                    it.copy(
                        addNewAlbumErrorMessage = consumed(),
                        addNewAlbumSuccessEvent = triggered(albumId)
                    )
                }
            }
        }
    }

    internal fun areAllAlbumsSelected(): Boolean {
        val userAlbums = uiState.value.albums.filter { it.mediaAlbum is MediaAlbum.User }
        return userAlbums.size == uiState.value.selectedUserAlbums.size
    }

    internal fun selectAllAlbums() {
        val userAlbums = uiState.value.albums
            .filter { it.mediaAlbum is MediaAlbum.User }
            .mapNotNull { it.mediaAlbum as? MediaAlbum.User }
            .toSet()

        uiState.update {
            it.copy(selectedUserAlbums = userAlbums)
        }
    }

    internal fun clearAlbumsSelection() {
        uiState.update {
            it.copy(selectedUserAlbums = emptySet())
        }
    }

    internal fun toggleAlbumSelection(album: MediaAlbum.User) {
        uiState.update { state ->
            val currentSelected = state.selectedUserAlbums
            val newSelected = if (currentSelected.contains(album)) {
                currentSelected - album
            } else {
                currentSelected + album
            }
            state.copy(selectedUserAlbums = newSelected)
        }
    }

    internal fun handleSelectionAction(action: MenuActionWithIcon) {
        when (action) {
            AlbumSelectionAction.ManageLink -> {
                val navKey = AlbumGetMultipleLinksNavKey(
                    albumIds = uiState.value.selectedUserAlbums.map { it.id.id }.toSet(),
                    hasSensitiveContent = true
                )

                uiState.update { it.copy(navigationEvent = triggered(navKey)) }
                clearAlbumsSelection()
            }

            AlbumSelectionAction.Delete -> {
                uiState.update {
                    it.copy(deleteAlbumsConfirmationEvent = triggered)
                }
            }
        }
    }

    internal fun deleteAlbums() {
        viewModelScope.launch {
            val selectedAlbums = uiState.value.selectedUserAlbums

            runCatching {
                removeAlbumsUseCase(selectedAlbums.map { it.id })
            }.onFailure {
                Timber.e(it)
            }.onSuccess {
                if (selectedAlbums.size == 1) {
                    snackbarEventQueue.queueMessage(
                        sharedR.string.delete_singular_album_confirmation_message,
                        selectedAlbums.firstOrNull()?.title.orEmpty()
                    )
                } else {
                    snackbarEventQueue.queueMessage(
                        sharedR.string.albums_multiple_delete_success_message,
                        selectedAlbums.size
                    )
                }
            }

            clearAlbumsSelection()
        }
    }

    internal fun resetNavigationEvent() {
        uiState.update { it.copy(navigationEvent = consumed()) }
    }

    internal fun resetDeleteAlbumsConfirmationEvent() {
        uiState.update { it.copy(deleteAlbumsConfirmationEvent = consumed) }
    }


    internal fun resetErrorMessage() {
        uiState.update { it.copy(addNewAlbumErrorMessage = consumed()) }
    }

    internal fun resetAddNewAlbumSuccess() {
        uiState.update { it.copy(addNewAlbumSuccessEvent = consumed()) }
    }
}