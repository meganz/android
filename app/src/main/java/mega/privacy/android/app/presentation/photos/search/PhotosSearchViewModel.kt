package mega.privacy.android.app.presentation.photos.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.photos.albums.AlbumTitleStringMapper
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.RetrievePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.SavePhotosRecentQueriesUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class PhotosSearchViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val albumTitleStringMapper: AlbumTitleStringMapper,
    private val retrievePhotosRecentQueriesUseCase: RetrievePhotosRecentQueriesUseCase,
    private val savePhotosRecentQueriesUseCase: SavePhotosRecentQueriesUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {
    private val _state: MutableStateFlow<PhotosSearchState> = MutableStateFlow(PhotosSearchState())

    val state: StateFlow<PhotosSearchState> = _state.asStateFlow()

    private var showHiddenItems: Boolean = false

    fun initialize(
        albumsFlow: Flow<List<UIAlbum>>,
        photosFlow: Flow<List<Photo>>,
    ) {
        albumsFlow
            .onEach(::updateAlbums)
            .launchIn(viewModelScope)

        photosFlow
            .onEach(::updatePhotos)
            .launchIn(viewModelScope)

        retrieveQueries()

        viewModelScope.launch {
            if (!getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)) {
                showHiddenItems = true
                return@launch
            }

            monitorAccountDetailUseCase()
                .onEach(::handleAccountDetails)
                .launchIn(viewModelScope)

            monitorShowHiddenItemsUseCase()
                .onEach(::handleHiddenItemsVisibility)
                .launchIn(viewModelScope)
        }
    }

    private fun updateAlbums(albums: List<UIAlbum>) {
        _state.update {
            it.copy(albumsSource = albums)
        }
        searchAlbums(query = _state.value.query)
    }

    private fun updatePhotos(photos: List<Photo>) {
        _state.update {
            it.copy(photosSource = photos)
        }
        searchPhotos(query = _state.value.query)
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
        updateRecentQueries(query)
    }

    private fun searchAlbums(query: String) = viewModelScope.launch(defaultDispatcher) {
        val albums = if (query.isBlank()) {
            listOf()
        } else {
            _state.value.albumsSource.filter {
                albumTitleStringMapper(it.title).contains(query, ignoreCase = true)
            }
        }

        _state.update {
            it.copy(
                albums = albums,
                isSearchingAlbums = false,
            )
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
            )
        }
    }

    private fun updateRecentQueries(query: String) = viewModelScope.launch(defaultDispatcher) {
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
}
