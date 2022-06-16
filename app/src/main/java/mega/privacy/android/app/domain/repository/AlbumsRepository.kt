package mega.privacy.android.app.domain.repository

/**
 * Albums repository
 */
interface AlbumsRepository {

    /**
     * Get Camera Upload Folder handle
     *
     * @return
     */
    suspend fun getCameraUploadFolderId(): Long?

    /**
     * Get Media Upload Folder handle
     *
     * @return
     */
    suspend fun getMediaUploadFolderId(): Long?
}