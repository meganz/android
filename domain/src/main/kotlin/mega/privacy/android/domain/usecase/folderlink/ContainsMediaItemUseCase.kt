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
     */
    operator fun invoke(list: List<TypedNode>) =
        list.firstOrNull {
            runCatching { it as TypedFileNode }.getOrNull()?.let {
                it.type !is SvgFileTypeInfo && (it.type is ImageFileTypeInfo || it.type is VideoFileTypeInfo)
            } == true
        } != null
}