package mega.privacy.android.domain.repository

/**
 * Folder repository
 */
interface FolderRepository {

    /**
     * @param isPrimary whether the primary upload's folder is returned
     * @return the primary or secondary upload folder's handle
     */
    suspend fun getUploadFolderHandle(isPrimary: Boolean): Long
}