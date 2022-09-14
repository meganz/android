package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.FileGalleryItem

/**
 * The use case interface to get all gallery files
 */
fun interface GetAllGalleryFiles {
    /**
     * get favourites
     * @return Flow<List<FileGalleryItem>>
     */
    operator fun invoke(): Flow<List<FileGalleryItem>>
}