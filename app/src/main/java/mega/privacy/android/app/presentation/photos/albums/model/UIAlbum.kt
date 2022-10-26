package mega.privacy.android.app.presentation.photos.albums.model

import android.content.Context
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

data class UIAlbum(
    val id: Album,
    val title: (Context) -> String,
    val count: Int,
    val coverPhoto: Photo?,
    val photos: List<Photo>,
)

/**
 * Get Album Photos
 */
fun List<UIAlbum>.getAlbumPhotos(id: Album): List<Photo> =
    this.first { it.id == id }.photos