package mega.privacy.android.feature.photos.provider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum

@Deprecated("PhotosCache should be removed later")
object PhotosCache {
    val photosFlow: MutableStateFlow<List<Photo>> = MutableStateFlow(listOf())

    val albumsFlow: MutableStateFlow<List<UIAlbum>> = MutableStateFlow(listOf())

    fun updatePhotos(photos: List<Photo>) = photosFlow.update { photos }

    fun updateAlbums(albums: List<UIAlbum>) = albumsFlow.update { albums }
}