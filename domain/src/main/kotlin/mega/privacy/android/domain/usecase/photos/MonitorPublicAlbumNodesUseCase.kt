package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.AlbumRepository
import javax.inject.Inject

/**
 * Use case to monitor public album sharing nodes
 */
class MonitorPublicAlbumNodesUseCase @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    operator fun invoke(): Flow<List<ImageNode>> = flow {
        emit(getPublicAlbumNodes())
    }

    private suspend fun getPublicAlbumNodes(): List<ImageNode> {
        return albumRepository.getPublicImageNodes()
    }
}
