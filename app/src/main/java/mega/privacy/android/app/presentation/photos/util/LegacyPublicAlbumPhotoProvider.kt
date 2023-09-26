package mega.privacy.android.app.presentation.photos.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.GetPreviewElementNodeListenerInterface
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Deprecated(
    message = "Temporary provider to serve public album use cases due to limited legacy architecture around" +
            "preview and download node design which both take MegaNode as parameter." +
            "Once preview and download design are refactored following architecture guideline," +
            "this provider will be refactored too.",
)
@Singleton
class LegacyPublicAlbumPhotoNodeProvider @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val publicNodesMap: MutableMap<NodeId, MegaNode> = mutableMapOf()

    suspend fun loadNodeCache(albumPhotoIds: List<AlbumPhotoId>) {
        publicNodesMap.clear()

        return withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val nodeAlbumPhotoIdMap = albumPhotoIds.associateBy(
                    keySelector = { it.nodeId.longValue },
                    valueTransform = { it },
                )

                val listener = GetPreviewElementNodeListenerInterface(
                    nodeAlbumPhotoIdMap = nodeAlbumPhotoIdMap,
                    onCompletion = { nodeAlbumPhotoIdPairs ->
                        launch {
                            nodeAlbumPhotoIdPairs.forEach { (node, _) ->
                                publicNodesMap[NodeId(node.handle)] = node
                            }
                            continuation.resume(Unit)
                        }
                    }
                )

                if (albumPhotoIds.isNotEmpty()) {
                    for (albumPhotoId in albumPhotoIds) {
                        megaApiGateway.getPreviewElementNode(
                            eid = albumPhotoId.id,
                            listener = listener,
                        )
                    }
                } else {
                    continuation.resume(Unit)
                }

                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
    }

    fun getPublicNodes(): List<MegaNode> = publicNodesMap.map { (_, node) -> node }

    fun getPublicNode(handle: Long): MegaNode? = publicNodesMap[NodeId(handle)]

    fun stopPreview() = megaApiGateway.stopPublicSetPreview()
}
