package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.imagepreview.mapper.OfflineFileInformationToImageNodeMapper
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import javax.inject.Inject

/**
 * Get offline nodes use case
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorOfflineImageNodesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val photosRepository: PhotosRepository,
    private val getOfflineFileInformationByIdUseCase: GetOfflineFileInformationByIdUseCase,
    private val offlineFileInformationToImageNodeMapper: OfflineFileInformationToImageNodeMapper,
) {
    /**
     * Invoke the use case
     * @param path The path of the node
     * @param filterSvg Filter SVG
     */
    operator fun invoke(path: String, filterSvg: Boolean = false): Flow<List<ImageNode>> =
        nodeRepository
            .monitorOfflineNodeUpdates()
            .mapLatest { getImageNodes(path = path, offline = it, filterSvg = filterSvg) }

    private suspend fun getImageNodes(
        path: String,
        offline: List<Offline>,
        filterSvg: Boolean,
    ) = offline.mapAsync {
        if (it.path != path || it.isFolder) return@mapAsync null
        it.handle.toLongOrNull()?.let { handle ->
            photosRepository.fetchImageNode(
                nodeId = NodeId(handle),
                filterSvg = filterSvg,
            ) ?: getOfflineFileInformationByIdUseCase(
                nodeId = NodeId(handle),
                useOriginalImageAsThumbnail = true
            )?.let { info ->
                offlineFileInformationToImageNodeMapper(info, filterSvg)
            }
        }
    }.filterNotNull()
}
