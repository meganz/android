package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import javax.inject.Inject

/**
 * Wraps a [TypedFileNode] that already loaded in memory for the timeline grid as a
 * lightweight [ImageNode] for the image viewer.
 */
internal class TypedFileNodeToImageNodeMapper @Inject constructor(
    private val thumbnailFromServerMapper: ThumbnailFromServerMapper,
    private val previewFromServerMapper: PreviewFromServerMapper,
    private val fullImageFromServerMapper: FullImageFromServerMapper,
    private val megaApiGateway: MegaApiGateway,
) {
    operator fun invoke(node: TypedFileNode): ImageNode =
        object : ImageNode, FileNode by node {
            // Match ImageNodeMapper: the viewer expects these to be null for online
            // nodes and resolves the real paths on demand. TypedFileNode/FileNodeMapper
            // populates them with (possibly non-existent) cache paths, which would make
            // ImagePreviewVideoLauncher treat a streamed video as a local file.
            override val thumbnailPath: String? = null
            override val previewPath: String? = null
            override val fullSizePath: String? = null

            override val downloadThumbnail: suspend (String) -> String = { path ->
                thumbnailFromServerMapper(requireMegaNode()).invoke(path)
            }

            override val downloadPreview: suspend (String) -> String = { path ->
                previewFromServerMapper(requireMegaNode()).invoke(path)
            }

            override val downloadFullImage: (String, Boolean, () -> Unit) -> Flow<ImageProgress> =
                { path, highPriority, resetDownloads ->
                    flow {
                        emitAll(
                            fullImageFromServerMapper(requireMegaNode())
                                .invoke(path, highPriority, resetDownloads)
                        )
                    }
                }

            override val latitude: Double = 0.0

            override val longitude: Double = 0.0

            private suspend fun requireMegaNode() =
                megaApiGateway.getMegaNodeByHandle(node.id.longValue)
                    ?: throw IllegalStateException("MegaNode not found for handle ${node.id.longValue}")
        }
}
