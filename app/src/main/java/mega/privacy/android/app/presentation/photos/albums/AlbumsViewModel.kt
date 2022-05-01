package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetFavouriteAlbumItems
import mega.privacy.android.app.presentation.extensions.toAlbumCoverList
import mega.privacy.android.app.presentation.photos.model.AlbumsLoadState
import mega.privacy.android.app.usecase.MegaException
import javax.inject.Inject

/**
 * AlbumsViewModel handle albums cover page view logic
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    //val sortOrderManagement: SortOrderManagement,
    private val getAllFavorites: GetFavouriteAlbumItems,
) : ViewModel() {

    private val _favouritesState =
        MutableStateFlow<AlbumsLoadState>(AlbumsLoadState.Loading)
    val favouritesState = _favouritesState.asStateFlow()
    /**
     * Get current sort rule from SortOrderManagement
     */
    //fun getOrder() = sortOrderManagement.getOrderCamera()

    init {
        getAllFavourites()
    }

    private var currentNodeJob: Job? = null

    /**
     * Get all favourites
     */
    private fun getAllFavourites() {
        _favouritesState.update {
            AlbumsLoadState.Loading
        }
        // Cancel the previous job avoid to repeatedly observed
        currentNodeJob?.cancel()
        currentNodeJob = viewModelScope.launch {
            runCatching {
                getAllFavorites()
            }.onSuccess { flow ->
                flow.collect { list ->
                    val newAlbumList = list.toAlbumCoverList()
                    _favouritesState.update {
                        AlbumsLoadState.Success(newAlbumList)
                    }
                }
            }.onFailure { exception ->
                if (exception is MegaException) {
                    _favouritesState.update {
                        AlbumsLoadState.Error(exception)
                    }
                }
            }
        }
    }
}