package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FolderInfoMapper
import mega.privacy.android.data.mapper.FolderLoginStatusMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.domain.entity.FolderInfo
import mega.privacy.android.domain.entity.folderlink.FetchNodeRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.exception.SynchronisationException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FolderLinkRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Implementation of [FolderLinkRepository]
 */
internal class FolderLinkRepositoryImpl @Inject constructor(
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaApiGateway: MegaApiGateway,
    private val folderLoginStatusMapper: FolderLoginStatusMapper,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val nodeMapper: NodeMapper,
    private val folderInfoMapper: FolderInfoMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FolderLinkRepository {

    override suspend fun fetchNodes() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(
                                Result.success(
                                    FetchNodeRequestResult(
                                        request.nodeHandle,
                                        request.flag
                                    )
                                )
                            )
                        }

                        MegaError.API_EBLOCKED -> {
                            continuation.resumeWith(Result.failure(FetchFolderNodesException.LinkRemoved()))
                        }

                        MegaError.API_ETOOMANY -> {
                            continuation.resumeWith(Result.failure(FetchFolderNodesException.AccountTerminated()))
                        }

                        else -> {
                            continuation.resumeWith(Result.failure(FetchFolderNodesException.GenericError()))
                        }
                    }
                }
            )
            megaApiFolderGateway.fetchNodes(listener)
            continuation.invokeOnCancellation {
                megaApiFolderGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun updateLastPublicHandle(nodeHandle: Long) = withContext(ioDispatcher) {
        if (nodeHandle != MegaApiJava.INVALID_HANDLE) {
            megaLocalStorageGateway.setLastPublicHandle(nodeHandle)
            megaLocalStorageGateway.setLastPublicHandleTimeStamp()
        }
    }

    override suspend fun loginToFolder(folderLink: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _: MegaRequest, error: MegaError ->
                    continuation.resumeWith(Result.success(folderLoginStatusMapper(error)))
                }
            )
            megaApiFolderGateway.loginToFolder(folderLink, listener)
            continuation.invokeOnCancellation {
                megaApiFolderGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getFolderLinkNode(handle: String): UnTypedNode =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(megaApiGateway.base64ToHandle(handle))?.let {
                convertToUntypedNode(it)
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }

    override suspend fun getRootNode(): UnTypedNode? = withContext(ioDispatcher) {
        megaApiFolderGateway.getRootNode()?.let { convertToUntypedNode(it) }
    }

    @Throws(SynchronisationException::class)
    override suspend fun getParentNode(nodeId: NodeId): UnTypedNode = withContext(ioDispatcher) {
        megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
            megaApiFolderGateway.getParentNode(node)?.let { convertToUntypedNode(it) }
        } ?: throw SynchronisationException("Non null node found be null when fetched from api")
    }

    @Throws(SynchronisationException::class)
    override suspend fun getNodeChildren(handle: Long, order: Int?): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(handle)?.let { parent ->
                megaApiFolderGateway.getChildrenByNode(parent, order)
                    .map { convertToUntypedNode(it) }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }
    }

    private suspend fun convertToUntypedNode(node: MegaNode): UnTypedNode =
        nodeMapper(node, fromFolderLink = true)

    override suspend fun getPublicLinkInformation(folderLink: String): FolderInfo =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("getPublicLinkInformation") {
                    folderInfoMapper(
                        megaFolderInfo = it.megaFolderInfo,
                        folderName = it.text
                    )
                }

                megaApiFolderGateway.getPublicLinkInformation(folderLink, listener)
                continuation.invokeOnCancellation {
                    megaApiFolderGateway.removeRequestListener(listener)
                }
            }
        }
}