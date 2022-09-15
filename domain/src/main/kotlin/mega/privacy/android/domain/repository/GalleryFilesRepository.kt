package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.FileGalleryItem

/**
 * Repository for get gallery files
 */
interface GalleryFilesRepository {

    /**
     * Gets a fow of list of all images of gallery
     *
     * @return: Flow of List of @FileGalleryItem
     */
    fun getAllGalleryImages(): Flow<FileGalleryItem>

    /**
     * Gets a fow of list of all videos of gallery
     *
     * @return: Flow of List of @FileGalleryItem
     */
    fun getAllGalleryVideos(): Flow<FileGalleryItem>

}