package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The implementation of [GetVideosByParentHandleFromMegaApiFolder]
 */
class DefaultGetVideosByParentHandleFromMegaApiFolder @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val addNodeType: AddNodeType,
) : GetVideosByParentHandleFromMegaApiFolder {
    override suspend fun invoke(parentHandle: Long, order: SortOrder): List<TypedNode>? =
        mediaPlayerRepository.getVideosByParentHandleFromMegaApiFolder(parentHandle, order)?.map {
            addNodeType(it)
        }

}