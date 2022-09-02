package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get timeline photos
 *
 * @property photosRepository
 */
class DefaultGetTimelinePhotos @Inject constructor(
    private val photosRepository: PhotosRepository,
) : GetTimelinePhotos {

    override fun invoke(): Flow<List<Photo>> = flow {
        emit(photosRepository.searchMegaPhotos())
        emitAll(getUpdatePhotosFlow())
    }

    private fun getUpdatePhotosFlow(): Flow<List<Photo>> = photosRepository.monitorNodeUpdates()
        .map { (changes) ->
            changes
                .filter { (_, value) ->
                    value.contains(NodeChanges.New)
                            || value.contains(NodeChanges.Favourite)
                            || value.contains(NodeChanges.Attributes)
                            || value.contains(NodeChanges.Parent)
                }
        }
        .filter { it.isNotEmpty() }
        .map { photosRepository.searchMegaPhotos() }

}