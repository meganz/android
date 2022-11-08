package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Album

/**
 * Use Case to create an album
 */
fun interface CreateAlbum {
    /**
     * The invoke method
     *
     * @param name
     */
    suspend operator fun invoke(name: String): Album.UserAlbum
}