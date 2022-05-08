package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.presentation.extensions.*
import mega.privacy.android.app.presentation.photos.model.AlbumCoverItem
import mega.privacy.android.app.presentation.photos.model.AlbumsLoadState
import mega.privacy.android.app.usecase.MegaException
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import javax.inject.Inject

/**
 * AlbumsViewModel handle albums cover page view logic
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getAllFavorites: GetAllFavorites,
    private val cameraUploadFolder: GetCameraUploadFolder,
    private val mediaUploadFolder: GetMediaUploadFolder,
    private val getThumbnail: GetThumbnail
) : ViewModel() {

    private val _favouritesState =
        MutableStateFlow<AlbumsLoadState>(AlbumsLoadState.Empty(listOf(createEmptyFavAlbum())))
    val favouritesState = _favouritesState.asStateFlow()

    init {
        getFavouriteAlbumCover()
    }

    private var currentNodeJob: Job? = null

    /**
     * Get all favourites
     */
    private fun getFavouriteAlbumCover() {
        _favouritesState.update {
            AlbumsLoadState.Loading
        }
        currentNodeJob = viewModelScope.launch {
            getAllFavorites()
                .onCompletion { error ->
                    if (error is MegaException) {
                        _favouritesState.update {
                            AlbumsLoadState.Error(error)
                        }
                    }
                }
                .collectLatest { favList ->
                    if (favList.isEmpty())
                        return@collectLatest
                    val sortList = favList.filter {
                        it.node.isImage() || (it.node.isVideo() && isInSyncFolder(it.parentId))
                    }.sortedByDescending {
                        it.modificationTime
                    }
                    val newList = ArrayList<AlbumCoverItem>()
                    val latestFavouriteItem = sortList[0]
                    val thumbnail =
                        getThumbnail(latestFavouriteItem.id, latestFavouriteItem.base64Id)
                    val favoriteAlbum =
                        latestFavouriteItem.toFavoriteAlbumCoverItem(thumbnail, sortList.size)
                    newList.add(favoriteAlbum)
                    _favouritesState.update {
                        AlbumsLoadState.Success(newList)
                    }
                }
        }
    }


    /**
     * Check the file is in Camera Uploads(CU) or Media Uploads(MU) folder, if it is in, the parent handle will be camSyncHandle or secondaryMediaFolderEnabled
     *
     * @return True, the file is in CU or MU folder. False, it is not in.
     */
    private fun isInSyncFolder(parentId: Long): Boolean {
        // check node in Camera Uploads Folder if camSyncHandle existed
        cameraUploadFolder()?.let { camSyncHandle ->
            if (parentId == camSyncHandle.toLong())
                return true
        }
        //  check node in  Media Uploads Folder if megaHandleSecondaryFolder handle existed
        mediaUploadFolder()?.let { megaHandleSecondaryFolder ->
            if (parentId == megaHandleSecondaryFolder.toLong())
                return true
        }
        return false
    }

}