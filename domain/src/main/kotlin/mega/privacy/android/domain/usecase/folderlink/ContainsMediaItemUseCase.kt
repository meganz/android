package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Use case to check if the list of node has image/video media item
 */
class ContainsMediaItemUseCase @Inject constructor() {

    /**
     * Invoke
     *
     * @param list List of [TypedNode]
     * @return true if the list contains image/video media item, false otherwise
     */
    operator fun invoke(list: List<TypedNode>) = list.any { node ->
        (node as? TypedFileNode)?.let { fileNode ->
            (fileNode.type is ImageFileTypeInfo || fileNode.type is VideoFileTypeInfo) &&
                    fileNode.type !is SvgFileTypeInfo
        } == true
    }
}