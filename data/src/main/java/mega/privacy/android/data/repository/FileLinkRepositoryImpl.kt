package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileLinkRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [FileLinkRepository]
 */
internal class FileLinkRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val nodeMapper: NodeMapper,
    private val cacheGateway: CacheGateway,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FileLinkRepository {

    override suspend fun getPublicNode(url: String) = withContext(ioDispatcher) {
        val publicNode = getPublicMegaNode(nodeFileLink = url)
        if (publicNode.handle != MegaApiJava.INVALID_HANDLE) {
            megaLocalStorageGateway.setLastPublicHandle(publicNode.handle)
            megaLocalStorageGateway.setLastPublicHandleTimeStamp()
        }

        val previewPath = getPreviewPath(publicNode)
        val node = nodeMapper(publicNode)
        (node as? DefaultFileNode)?.copy(previewPath = previewPath) ?: node
    }

    private suspend fun getPreviewPath(node: MegaNode) = withContext(ioDispatcher) {
        if (node.hasPreview()) {
            val previewFile =
                runCatching { getPreviewFromLocal(node) ?: getPreviewFromServer(node) }.getOrNull()
            previewFile?.takeIf { it.exists() }?.absolutePath
        } else {
            null
        }
    }

    private suspend fun getPreviewFromLocal(node: MegaNode): File? =
        withContext(ioDispatcher) {
            getPreviewFile(node).takeIf { it?.exists() ?: false }
        }

    private suspend fun getPreviewFromServer(node: MegaNode): File? =
        withContext(ioDispatcher) {
            getPreviewFile(node)?.let { preview ->
                suspendCancellableCoroutine { continuation ->
                    megaApiGateway.getPreview(node, preview.absolutePath,
                        OptionalMegaRequestListenerInterface(
                            onRequestFinish = { _, error ->
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(preview))
                                } else {
                                    continuation.failWithError(error, "getPreviewFromServer")
                                }
                            }
                        )
                    )
                }
            }
        }

    private suspend fun getPreviewFile(node: MegaNode): File? =
        cacheGateway.getCacheFile(
            CacheFolderConstant.PREVIEW_FOLDER,
            "${node.base64Handle}${FileConstant.JPG_EXTENSION}"
        )

    override suspend fun encryptLinkWithPassword(link: String, password: String): String =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("encryptLinkWithPassword") {
                    it.text
                }
                megaApiGateway.encryptLinkWithPassword(link, password, listener)
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }

    override suspend fun getFileUrlByPublicLink(link: String): String? =
        getPublicMegaNode(link).let { node ->
            megaApiGateway.httpServerGetLocalLink(node)
        }

    private suspend fun getPublicMegaNode(nodeFileLink: String): MegaNode =
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(request.publicMegaNode))
                        }

                        MegaError.API_EBLOCKED -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.LinkRemoved()))
                        }

                        MegaError.API_ETOOMANY -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.AccountTerminated()))
                        }

                        MegaError.API_EINCOMPLETE -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.DecryptionKeyRequired()))
                        }

                        else -> {
                            continuation.resumeWith(Result.failure(PublicNodeException.GenericError()))
                        }
                    }
                }
            )
            megaApiGateway.getPublicNode(nodeFileLink, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
}