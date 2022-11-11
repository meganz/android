package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.photos.Photo

/**
 * Get Photos from a folder
 */
interface GetPhotosByFolderId {

    /**
     * Get Photos from a folder
     *
     * @param folderId
     * @return photo
     */
    operator fun invoke(folderId: Long, order: SortOrder): Flow<List<Photo>>
}