package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.FileGalleryItem

/**
 * Repository for get gallery files
 */
interface GalleryFilesRepository {

    /**
     * Gets a fow of list of all files of gallery
     * @return: Flow of List of @FileGalleryItem
     */
    suspend fun getAllGalleryFiles(): Flow<List<FileGalleryItem>>
}