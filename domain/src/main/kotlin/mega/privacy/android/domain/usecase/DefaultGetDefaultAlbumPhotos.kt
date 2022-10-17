package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get albums
 *
 * @property photosRepository
 */
class DefaultGetDefaultAlbumPhotos @Inject constructor(
    private val photosRepository: PhotosRepository,
) : GetDefaultAlbumPhotos {

    override fun invoke(list: List<suspend (Photo) -> Boolean>) = flow {
        emit(createAlbumList(list))
        emitAll(getUpdatePhotos(list))
    }

    private suspend fun createAlbumList(list: List<suspend (Photo) -> Boolean>) =
        photosRepository.searchMegaPhotos()
            .filter {
                list.any { predicate -> predicate(it) }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getUpdatePhotos(list: List<suspend (Photo) -> Boolean>) =
        photosRepository.monitorNodeUpdates()
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
            .mapLatest { createAlbumList(list) }

}