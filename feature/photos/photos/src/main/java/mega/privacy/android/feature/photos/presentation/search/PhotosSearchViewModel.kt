package mega.privacy.android.feature.photos.presentation.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.RetrievePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.SavePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhotosSearchViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val retrievePhotosRecentQueriesUseCase: RetrievePhotosRecentQueriesUseCase,
    private val savePhotosRecentQueriesUseCase: SavePhotosRecentQueriesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val albumsProvider: Lazy<Set<@JvmSuppressWildcards AlbumsDataProvider>>,
    private val albumUiStateMapper: AlbumUiStateMapper,
    private val monitorTimelinePhotosUseCase: Lazy<MonitorTimelinePhotosUseCase>,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state: MutableStateFlow<PhotosSearchState> = MutableStateFlow(PhotosSearchState())

    val state: StateFlow<PhotosSearchState> = _state
        .map { it.copy(contentState = computeContentState(it)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PhotosSearchState())

    private var showHiddenItems: Boolean = false

    init {
        monitorAccountDetail()
        monitorShowHiddenItems()
        initialize()
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach(::handleAccountDetails)
            .launchIn(viewModelScope)
    }

    private fun monitorShowHiddenItems() {
        monitorShowHiddenItemsUseCase()
            .onEach(::handleHiddenItemsVisibility)
            .launchIn(viewModelScope)
    }

    private fun initialize() {
        monitorAlbums()
        monitorPhotos()
        retrieveQueries()
    }

    private fun monitorAlbums() {
        monitorAlbumsFlow()
            .map { it.map(albumUiStateMapper::invoke) }
            .onEach(::updateAlbumUi)
            .launchIn(viewModelScope)
    }

    private fun monitorAlbumsFlow() = combine(
        flows = albumsProvider.get()
            .sortedBy { it.order }
            .map { it.monitorAlbums() },
        transform = { albums -> albums.toList().flatten() }
    )

    private fun updateAlbumUi(albums: List<AlbumUiState>) {
        _state.update {
            it.copy(albumSource = albums)
        }
        searchAlbums(query = _state.value.query)
    }

    private fun monitorPhotos() {
        monitorTimelinePhotosUseCase
            .get()
            .invoke(TimelinePhotosRequest())
            .catch { Timber.e(it) }
            .map { result -> result.allPhotos.map { it.photo } }
            .onEach(::updatePhotos)
            .launchIn(viewModelScope)
    }

    private fun updatePhotos(photos: List<Photo>) {
        _state.update {
            it.copy(photosSource = photos)
        }
        searchPhotos(query = _state.value.query)
    }

    fun updateQuery(query: String) {
        _state.update {
            it.copy(query = query)
        }
    }

    fun search(query: String) {
        _state.update {
            it.copy(
                query = query,
                photos = it.photos.takeIf { query.isNotBlank() }.orEmpty(),
                isSearchingPhotos = true,
                albums = it.albums.takeIf { query.isNotBlank() }.orEmpty(),
                isSearchingAlbums = true,
            )
        }

        searchAlbums(query)
        searchPhotos(query)
    }

    private fun searchAlbums(query: String) {
        viewModelScope.launch(defaultDispatcher) {
            val albums = if (query.isBlank()) {
                listOf()
            } else {
                _state.value.albumSource.filter {
                    it.title.get(context).contains(query, ignoreCase = true)
                }
            }

            _state.update {
                it.copy(
                    albums = albums,
                    isSearchingAlbums = false,
                )
            }
        }
    }

    private fun searchPhotos(query: String) = viewModelScope.launch(defaultDispatcher) {
        val accountType = _state.value.accountType
        val isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired

        val photos = if (query.isBlank()) {
            listOf()
        } else {
            _state.value.photosSource.filter {
                it.name.contains(query, ignoreCase = true)
            }.let { photos ->
                if (showHiddenItems || accountType?.isPaid == false || isBusinessAccountExpired) {
                    photos
                } else {
                    photos.filter { !it.isSensitive && !it.isSensitiveInherited }
                }.sortedWith(compareByDescending<Photo> { it.modificationTime }
                    .thenByDescending { it.id })
            }
        }

        _state.update {
            it.copy(
                photos = photos,
                isSearchingPhotos = false,
            )
        }
    }

    fun updateRecentQueries(query: String) = viewModelScope.launch(defaultDispatcher) {
        if (query.isBlank()) return@launch

        val recentQueries = (listOf(query) + _state.value.recentQueries).toSet().take(6)
        _state.update {
            it.copy(recentQueries = recentQueries)
        }
    }

    private fun handleHiddenItemsVisibility(isVisible: Boolean) {
        showHiddenItems = isVisible
        search(query = _state.value.query)
    }

    private suspend fun handleAccountDetails(accountDetail: AccountDetail) {
        val isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired

        _state.update {
            it.copy(
                accountType = accountDetail.levelDetail?.accountType,
                isBusinessAccountExpired = isBusinessAccountExpired,
            )
        }
    }

    fun updateSelectedQuery(query: String?) {
        _state.update {
            it.copy(selectedQuery = query)
        }
    }

    private fun retrieveQueries() = viewModelScope.launch {
        runCatching {
            retrievePhotosRecentQueriesUseCase()
        }.onSuccess { queries ->
            _state.update {
                it.copy(
                    isInitializing = false,
                    recentQueries = queries,
                )
            }
        }.onFailure { throwable ->
            _state.update {
                it.copy(isInitializing = false)
            }
            Timber.e(throwable)
        }
    }

    fun saveQueries() = viewModelScope.launch {
        runCatching {
            val queries = _state.value.recentQueries
            savePhotosRecentQueriesUseCase(queries)
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Computes the content state based on the given state properties.
     * This mirrors the logic in PhotosSearchScreen for consistency.
     */
    private fun computeContentState(state: PhotosSearchState): MediaContentState = when {
        state.isInitializing || state.isSearchingAlbums || state.isSearchingPhotos -> MediaContentState.Loading
        state.query.isBlank() -> {
            if (state.recentQueries.isEmpty()) {
                MediaContentState.WelcomeEmpty
            } else {
                MediaContentState.RecentQueries
            }
        }

        state.photos.isEmpty() && state.albums.isEmpty() -> MediaContentState.NoResults
        else -> MediaContentState.SearchResults
    }
}
