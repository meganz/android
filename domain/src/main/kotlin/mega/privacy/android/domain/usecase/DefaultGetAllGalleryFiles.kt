package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.domain.repository.GalleryFilesRepository
import javax.inject.Inject

/**
 * The use case implementation class to get favourites
 * @param repository FavouritesRepository
 */
class DefaultGetAllGalleryFiles @Inject constructor(private val repository: GalleryFilesRepository) :
    GetAllGalleryFiles {

    override fun invoke(): Flow<List<FileGalleryItem>> =
        flow {
            emitAll(repository.getAllGalleryFiles())
        }
}