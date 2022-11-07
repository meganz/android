package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default implementation of the use case CreateAlbum
 */
class DefaultCreateAlbum @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val albumRepository: AlbumRepository,
) : CreateAlbum {
    override suspend fun invoke(name: String): Album.UserAlbum {
        val newUserSet = albumRepository.createAlbum(name)
        val coverPhoto = if (newUserSet.cover == null) {
            null
        } else {
            photosRepository.getPhotoFromNodeID(NodeId(newUserSet.cover!!))
        }
        return Album.UserAlbum(
            id = AlbumId(newUserSet.id),
            title = newUserSet.name,
            cover = coverPhoto,
            modificationTime = newUserSet.modificationTime
        )
    }
}