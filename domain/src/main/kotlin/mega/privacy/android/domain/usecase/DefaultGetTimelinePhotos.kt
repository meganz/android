package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get timeline photos
 *
 * @property photosRepository
 */
class DefaultGetTimelinePhotos @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository
) : GetTimelinePhotos {

    override fun invoke(): Flow<List<Photo>> = flow {
        emit(photosRepository.searchMegaPhotos())
        emitAll(getUpdatePhotos())
    }

    private fun getUpdatePhotos(): Flow<List<Photo>> = nodeRepository.monitorNodeUpdates()
        .filter(predicate)
        .map { photosRepository.searchMegaPhotos() }

    private val applicableChanges = listOf(
        NodeChanges.New,
        NodeChanges.Favourite,
        NodeChanges.Attributes,
        NodeChanges.Parent,
    )

    private val predicate: (NodeUpdate) -> Boolean =
        {
            it.changes.values
                .flatten()
                .intersect(applicableChanges)
                .isNotEmpty()
        }
}