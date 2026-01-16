package mega.privacy.android.feature.photos.presentation.albums.photosselection

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.model.AlbumFlow
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelinePhotosSource

data class AlbumPhotosSelectionState(
    val albumFlow: AlbumFlow = AlbumFlow.Creation,
    val album: Album.UserAlbum? = null,
    val isInvalidAlbum: Boolean = false,
    val albumPhotoIds: Set<Long> = setOf(),
    val sourcePhotos: List<Photo> = listOf(),
    val photos: List<Photo> = listOf(),
    val filteredPhotoIds: Set<Long> = setOf(),
    val photosNodeContentTypes: List<PhotosNodeContentType> = listOf(),
    val selectedPhotoIds: Set<Long> = setOf(),
    val selectedLocation: TimelinePhotosSource = TimelinePhotosSource.ALL_PHOTOS,
    val isLocationDetermined: Boolean = false,
    val showFilterMenu: Boolean = false,
    val photosSelectionCompletedEvent: StateEventWithContent<Int> = consumed(),
    val accountType: AccountType? = null,
    val isLoading: Boolean = true,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
) {
    /**
     * Flag to check if all nodes are selected
     */
    val areAllNodesSelected: Boolean = photosNodeContentTypes
        .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
        .size == selectedPhotoIds.size
}

typealias PhotoDownload = suspend (
    photo: Photo,
    callback: (success: Boolean) -> Unit,
) -> Unit
