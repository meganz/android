package mega.privacy.android.feature.photos.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumSelectionAction
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumGetLinkNavKey
import mega.privacy.android.navigation.destination.AlbumGetMultipleLinksNavKey
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.contains
import kotlin.collections.minus
import kotlin.collections.plus
import kotlin.sequences.contains

@HiltViewModel
class AlbumsTabViewModel @Inject constructor(
    private val albumsProvider: Set<@JvmSuppressWildcards AlbumsDataProvider>,
    private val albumUiStateMapper: AlbumUiStateMapper,
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper,
    private val removeAlbumsUseCase: RemoveAlbumsUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val validateAndCreateUserAlbumUseCase: ValidateAndCreateUserAlbumUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {
    internal val uiState: StateFlow<AlbumsTabUiState>
        field = MutableStateFlow(AlbumsTabUiState())

    private var addNewAlbumJob: Job? = null

    init {
        monitorShowHiddenItems()
        monitorThemeMode()
        monitorAlbums()
    }

    private fun monitorShowHiddenItems() {
        monitorShowHiddenItemsUseCase()
            .catch { Timber.e(it) }
            .onEach { showHiddenItems ->
                uiState.update { it.copy(showHiddenItems = showHiddenItems) }
            }
            .launchIn(viewModelScope)
    }

    private fun monitorThemeMode() {
        monitorThemeModeUseCase()
            .catch { Timber.e(it) }
            .onEach {
                uiState.update {
                    it.copy(themeMode = it.themeMode)
                }
            }
            .launchIn(viewModelScope)
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
        if (addNewAlbumJob?.isActive == true) return

        addNewAlbumJob = viewModelScope.launch {
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
                viewModelScope.launch {
                    val selectedAlbums = uiState.value.selectedUserAlbums
                    val navKey = if (selectedAlbums.size == 1) {
                        val albumId = selectedAlbums.first().id
                        AlbumGetLinkNavKey(albumId = albumId.id)
                    } else {
                        AlbumGetMultipleLinksNavKey(
                            albumIds = uiState.value.selectedUserAlbums.map { it.id.id }.toSet(),
                        )
                    }

                    uiState.update { it.copy(navigationEvent = triggered(navKey)) }
                    clearAlbumsSelection()
                }

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

    internal fun getPresetNewAlbumName(defaultName: String): String {
        val userAlbumNames = uiState.value.albums
            .mapNotNull { (it.mediaAlbum as? MediaAlbum.User)?.title }
        return getNextDefaultAlbumNameUseCase(defaultName, userAlbumNames)
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