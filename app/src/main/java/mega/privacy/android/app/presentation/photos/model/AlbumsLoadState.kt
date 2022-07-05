package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.domain.entity.Album

/**
 * The album list load state
 */
sealed interface AlbumsLoadState {
    /**
     * Get album list success
     * @param albums album list
     */
    data class Success(val albums: List<Album>) : AlbumsLoadState

    /**
     * Loading state
     */
    object Loading : AlbumsLoadState

    /**
     * album list is empty
     */
    object Empty : AlbumsLoadState
}