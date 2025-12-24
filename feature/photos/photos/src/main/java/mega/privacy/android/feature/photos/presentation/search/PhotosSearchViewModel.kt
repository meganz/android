package mega.privacy.android.feature.photos.presentation.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumTitleStringMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.feature.photos.provider.PhotosCache
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhotosSearchViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val albumTitleStringMapper: AlbumTitleStringMapper,
    private val retrievePhotosRecentQueriesUseCase: RetrievePhotosRecentQueriesUseCase,
    private val savePhotosRecentQueriesUseCase: SavePhotosRecentQueriesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val albumsProvider: Lazy<Set<@JvmSuppressWildcards AlbumsDataProvider>>,
    private val albumUiStateMapper: AlbumUiStateMapper,
    private val monitorTimelinePhotosUseCase: Lazy<MonitorTimelinePhotosUseCase>,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state: MutableStateFlow<PhotosSearchState> = MutableStateFlow(PhotosSearchState())

    val state: StateFlow<PhotosSearchState> = _state.asStateFlow()

    private var showHiddenItems: Boolean = false

    init {
        monitorAccountDetail()
        monitorShowHiddenItems()
        getSingleActivityEnabled()
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

    private fun getSingleActivityEnabled() {
        viewModelScope.launch {
            runCatching { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) }
                .onSuccess { isEnabled ->
                    _state.update {
                        it.copy(isSingleActivityEnabled = isEnabled)
                    }

                    initialize(isEnabled)
                }
        }
    }

    private fun initialize(isSingleActivityEnabled: Boolean) {
        monitorAlbums(isSingleActivityEnabled)
        monitorPhotos(isSingleActivityEnabled)
        retrieveQueries()
    }

    private fun monitorAlbums(isSingleActivityEnabled: Boolean) {
        if (isSingleActivityEnabled) {
            monitorAlbumsFlow()
                .map { it.map(albumUiStateMapper::invoke) }
                .onEach(::updateAlbumUi)
                .launchIn(viewModelScope)
        } else {
            PhotosCache.albumsFlow
                .onEach(::updateAlbums)
                .launchIn(viewModelScope)
        }
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

    private fun monitorPhotos(isSingleActivityEnabled: Boolean) {
        val photosFlow = if (isSingleActivityEnabled) {
            createSingleActivityPhotosFlow()
        } else {
            PhotosCache.photosFlow
        }

        photosFlow
            .onEach(::updatePhotos)
            .launchIn(viewModelScope)
    }

    private fun createSingleActivityPhotosFlow() =
        monitorTimelinePhotosUseCase
            .get()
            .invoke(TimelinePhotosRequest(isPaginationEnabled = false))
            .map { result -> result.allPhotos.map { it.photo } }

    private fun updateAlbums(albums: List<UIAlbum>) {
        _state.update {
            it.copy(legacyAlbumSource = albums)
        }
        searchAlbums(query = _state.value.query)
    }

    private fun updatePhotos(photos: List<Photo>) {
        _state.update {
            it.copy(photosSource = photos)
        }
        searchPhotos(query = _state.value.query)
    }

    fun updateQuery(query: String) {
        _state.update {
            it.copy(
                query = query,
                contentState = updateContentState()
            )
        }
    }

    fun search(query: String) {
        _state.update {
            it.copy(
                query = query,
                photos = it.photos.takeIf { query.isNotBlank() }.orEmpty(),
                isSearchingPhotos = true,
                legacyAlbums = it.legacyAlbums.takeIf { query.isNotBlank() }.orEmpty(),
                albums = it.albums.takeIf { query.isNotBlank() }.orEmpty(),
                isSearchingAlbums = true,
                contentState = updateContentState()
            )
        }

        searchAlbums(query)
        searchPhotos(query)
    }

    private fun searchAlbums(query: String) {
        viewModelScope.launch(defaultDispatcher) {
            when (state.value.isSingleActivityEnabled) {
                true -> {
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
                            contentState = updateContentState()
                        )
                    }
                }

                false -> {
                    searchLegacyAlbums(query)
                }

                else -> return@launch
            }
        }
    }

    private suspend fun searchLegacyAlbums(query: String) {
        withContext(defaultDispatcher) {
            val albums = if (query.isBlank()) {
                listOf()
            } else {
                _state.value.legacyAlbumSource.filter {
                    albumTitleStringMapper(it.title).contains(query, ignoreCase = true)
                }
            }

            _state.update {
                it.copy(
                    legacyAlbums = albums,
                    isSearchingAlbums = false,
                    contentState = updateContentState()
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
                val ext = ".${it.fileTypeInfo.extension}"
                it.name.replace(ext, "").contains(query, ignoreCase = true)
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
                contentState = updateContentState()
            )
        }
    }

    fun updateRecentQueries(query: String) = viewModelScope.launch(defaultDispatcher) {
        if (query.isBlank()) return@launch

        val recentQueries = (listOf(query) + _state.value.recentQueries).toSet().take(6)
        _state.update {
            it.copy(
                recentQueries = recentQueries,
                contentState = updateContentState()
            )
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
                    contentState = updateContentState()
                )
            }
        }.onFailure { throwable ->
            _state.update {
                it.copy(
                    isInitializing = false,
                    contentState = updateContentState()
                )
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
     * Updates the content state based on the current state properties.
     */
    private fun updateContentState(): MediaContentState {
        val state = this.state.value

        return when {
            state.query.isBlank() && state.recentQueries.isEmpty() -> MediaContentState.WelcomeEmpty
            state.query.isBlank() -> MediaContentState.RecentQueries

            isAlbumEmpty()
                    && !state.isSearchingAlbums
                    && state.photos.isEmpty()
                    && !state.isSearchingPhotos -> MediaContentState.NoResults

            else -> MediaContentState.SearchResults
        }
    }

    private fun isAlbumEmpty(): Boolean = when (state.value.isSingleActivityEnabled) {
        true -> state.value.albums.isEmpty()
        false -> state.value.legacyAlbums.isEmpty()
        else -> true
    }
}