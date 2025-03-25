package mega.privacy.android.app.presentation.photos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.domain.entity.photos.Photo

internal object PhotosCache {
    val photosFlow: MutableStateFlow<List<Photo>> = MutableStateFlow(listOf())

    val albumsFlow: MutableStateFlow<List<UIAlbum>> = MutableStateFlow(listOf())

    fun updatePhotos(photos: List<Photo>) = photosFlow.update { photos }

    fun updateAlbums(albums: List<UIAlbum>) = albumsFlow.update { albums }
}
