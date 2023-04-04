package mega.privacy.android.app.presentation.photos.albums.model

import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Data class UIAlbum
 *
 * @property id
 * @property title          The album title
 * @property count          The count of photos items inside the album
 * @property coverPhoto     The selected photos used as the cover
 * @property photos         The list of all photos in the albums
 */
data class UIAlbum(
    val id: Album,
    val title: AlbumTitle,
    val count: Int,
    val coverPhoto: Photo?,
    val photos: List<Photo>,
)

/**
 * Get Album Photos
 */
fun List<UIAlbum>.getAlbumPhotos(id: Album): List<Photo> =
    this.first { it.id == id }.photos