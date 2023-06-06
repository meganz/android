package mega.privacy.android.app.presentation.photos.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.GetPreviewElementNodeListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.PhotoMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
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
internal class LegacyPublicAlbumPhotoProvider @Inject constructor(
    private val photoMapper: PhotoMapper,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val publicNodesMap: MutableMap<NodeId, MegaNode> = mutableMapOf()

    suspend fun getPublicPhotos(albumPhotoIds: List<AlbumPhotoId>): List<Photo> {
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
                            val photos = nodeAlbumPhotoIdPairs.mapNotNull { (node, albumPhotoId) ->
                                publicNodesMap[NodeId(node.handle)] = node
                                photoMapper(node, albumPhotoId)
                            }
                            continuation.resume(photos)
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
                    continuation.resume(listOf())
                }

                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
    }

    suspend fun downloadPublicThumbnail(photo: Photo, callback: (Boolean) -> Unit) {
        withContext(ioDispatcher) {
            val node = publicNodesMap[NodeId(photo.id)]
            val thumbnailFilePath = photo.thumbnailFilePath

            if (thumbnailFilePath.isNullOrBlank()) {
                callback(false)
            } else if (File(thumbnailFilePath).exists()) {
                callback(true)
            } else if (node == null) {
                callback(false)
            } else {
                megaApiGateway.getThumbnail(
                    node = node,
                    thumbnailFilePath = thumbnailFilePath,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            callback(error.errorCode == MegaError.API_OK)
                        },
                    ),
                )
            }
        }
    }

    suspend fun downloadPublicPreview(photo: Photo, callback: (Boolean) -> Unit) {
        withContext(ioDispatcher) {
            val node = publicNodesMap[NodeId(photo.id)]
            val previewFilePath = photo.previewFilePath

            if (previewFilePath.isNullOrBlank()) {
                callback(false)
            } else if (File(previewFilePath).exists()) {
                callback(true)
            } else if (node == null) {
                callback(false)
            } else {
                megaApiGateway.getPreview(
                    node = node,
                    previewFilePath = previewFilePath,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            callback(error.errorCode == MegaError.API_OK)
                        },
                    ),
                )
            }
        }
    }

    fun getPublicNodes(): List<MegaNode> {
        return publicNodesMap.map { (_, node) -> node }
    }

    fun getPublicNode(handle: Long): MegaNode? {
        return publicNodesMap[NodeId(handle)]
    }
}
