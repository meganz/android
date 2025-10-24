package mega.privacy.android.domain.entity.media

import mega.privacy.android.domain.entity.photos.Photo

/**
 * Interface for system album
 */
interface SystemAlbum {
    /**
     * Name of the album
     */
    val albumName: String

    /**
     * Function to determine if a photo belongs to this album type
     */
    suspend fun filter(photo: Photo): Boolean
}
