package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get raw album nodes use case
 */
class MonitorRawAlbumNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<List<ImageNode>> = photosRepository.monitorImageNodes()
        .mapLatest { imageNodes ->
            imageNodes.filter { it.type is RawFileTypeInfo }
        }.flowOn(defaultDispatcher)
}
