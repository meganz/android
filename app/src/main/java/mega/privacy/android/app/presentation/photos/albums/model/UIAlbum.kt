package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Data class UIAlbum
 *
 * @property id
 * @property title          The album title
 * @property count          The count of photos items inside the album
 * @property imageCount     The count of images in the album
 * @property videoCount     The count of videos in the album
 * @property coverPhoto     The selected photos used as the cover
 * @property defaultCover   The fallback cover if main cover is null
 * @property photos         The list of all photos in the albums
 * @property isLoadingDone  To indicate if the loading of the UIAlbum is done
 */
data class UIAlbum(
    val id: Album,
    val title: AlbumTitle,
    val count: Int,
    val imageCount: Int,
    val videoCount: Int,
    val coverPhoto: Photo?,
    val defaultCover: Photo? = null,
    val photos: List<Photo>,
    val isLoadingDone: Boolean = false,
)

/**
 * Get Album Photos
 */
fun List<UIAlbum>.getAlbumPhotos(id: Album): List<Photo> =
    this.first { it.id == id }.photos