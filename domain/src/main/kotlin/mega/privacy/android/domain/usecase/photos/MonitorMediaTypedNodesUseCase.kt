package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class MonitorMediaTypedNodesUseCase @Inject constructor(private val photosRepository: PhotosRepository) {

    operator fun invoke() = photosRepository
        .monitorMediaTypedNodes
        .map { nodes ->
            nodes.asSequence()
                .filterIsInstance<TypedFileNode>()
                .filter { it.isAValidMediaNode() }
                .toList()
        }

    private fun TypedFileNode.isAValidMediaNode(): Boolean =
        (type is ImageFileTypeInfo && type !is SvgFileTypeInfo) || type is VideoFileTypeInfo
}
