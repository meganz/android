package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.domain.entity.FolderVersionInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaShare
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Default implementation of [MegaNodeRepository]
 *
 * @property context
 * @property megaApiGateway
 * @property megaApiFolderGateway
 * @property megaChatApiGateway
 * @property ioDispatcher
 * @property megaLocalStorageGateway
 * @property shareDataMapper
 * @property megaExceptionMapper
 * @property sortOrderIntMapper
 * @property nodeMapper
 * @property fileTypeInfoMapper
 * @property fileGateway
 * @property chatFilesFolderUserAttributeMapper
 * @property streamingGateway
 * @property getLinksSortOrder
 * @property cancelTokenProvider
 */
internal class MegaNodeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val shareDataMapper: ShareDataMapper,
    private val megaExceptionMapper: MegaExceptionMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val nodeMapper: NodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    private val streamingGateway: StreamingGateway,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val cancelTokenProvider: CancelTokenProvider,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
) : MegaNodeRepository {

    override suspend fun moveNode(
        nodeToMove: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("moveNode") {
                NodeId(it.nodeHandle)
            }
            megaApiGateway.moveNode(
                nodeToMove = nodeToMove,
                newNodeParent = newNodeParent,
                newNodeName = newNodeName,
                listener = listener
            )
        }
    }

    @Throws(MegaException::class)
    override suspend fun getRootFolderVersionInfo(): FolderVersionInfo =
        withContext(ioDispatcher) {
            val rootNode = megaApiGateway.getRootNode()
            suspendCoroutine { continuation ->
                megaApiGateway.getFolderInfo(
                    rootNode,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = onRequestFolderInfoCompleted(continuation)
                    )
                )
            }
        }

    private fun onRequestFolderInfoCompleted(continuation: Continuation<FolderVersionInfo>) =
        { request: MegaRequest, error: MegaError ->
            if (error.errorCode == MegaError.API_OK) {
                continuation.resumeWith(
                    Result.success(
                        with(request.megaFolderInfo) {
                            FolderVersionInfo(
                                numVersions,
                                versionsSize
                            )
                        }
                    )
                )
            } else {
                continuation.failWithError(error, "onRequestFolderInfoCompleted")
            }
        }

    override suspend fun getRootNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()
    }

    override suspend fun getBackupsNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getBackupsNode()
    }

    override suspend fun isNodeInBackups(megaNode: MegaNode) = withContext(ioDispatcher) {
        megaApiGateway.isInBackups(megaNode)
    }

    override suspend fun getRubbishBinNode(): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getRubbishBinNode()
    }

    override suspend fun isInRubbish(node: MegaNode): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isInRubbish(node)
    }

    override suspend fun getParentNode(node: MegaNode): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getParentNode(node)
    }

    override suspend fun getChildrenNode(parentNode: MegaNode, order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            val token = cancelTokenProvider.getOrCreateCancelToken()
            val filter = megaSearchFilterMapper(
                parentHandle = NodeId(parentNode.handle),
            )
            megaApiGateway.getChildren(filter, sortOrderIntMapper(order), token)
        }

    override suspend fun getNodeByPath(path: String?, megaNode: MegaNode?): MegaNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByPath(path, megaNode)
        }

    override suspend fun getNodeByHandle(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
    }

    override suspend fun getPublicNodeByHandle(handle: Long): MegaNode? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(handle)
        }

    override suspend fun getIncomingSharesNode(order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getIncomingSharesNode(sortOrderIntMapper(order))
        }

    override suspend fun getUserFromInShare(node: MegaNode, recursive: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.getUserFromInShare(node, recursive)
        }

    override suspend fun authorizeNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiFolderGateway.authorizeNode(handle)
    }

    @Deprecated(
        message = "getPublicLinks MegaNode implementation has been deprecated. Use untyped node implementation",
        replaceWith = ReplaceWith("mega.privacy.android.domain.repository.filemanagement.ShareRepository#getPublicLinks(SortOrder)")
    )
    override suspend fun getPublicLinks(order: SortOrder): List<MegaNode> =
        withContext(ioDispatcher) {
            megaApiGateway.getPublicLinks(sortOrderIntMapper(order))
        }

    override suspend fun isPendingShare(node: MegaNode): Boolean = withContext(ioDispatcher) {
        megaApiGateway.isPendingShare(node)
    }

    override suspend fun hasBackupsChildren(): Boolean = withContext(ioDispatcher) {
        megaApiGateway.getBackupsNode()?.let { megaApiGateway.hasChildren(it) } ?: false
    }


    override suspend fun checkAccessErrorExtended(node: MegaNode, level: Int): MegaException =
        withContext(ioDispatcher) {
            megaExceptionMapper(megaApiGateway.checkAccessErrorExtended(node, level))
        }

    override suspend fun createShareKey(megaNode: MegaNode) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener =
                continuation.getRequestListener("createShareKey") { return@getRequestListener }
            megaApiGateway.openShareDialog(megaNode, listener)
        }
    }

    override suspend fun getOutShares(nodeId: NodeId): List<MegaShare>? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
                megaApiGateway.getOutShares(node)
            }
        }

    override suspend fun search(
        nodeId: NodeId?,
        query: String,
        order: SortOrder,
        searchTarget: SearchTarget,
        searchCategory: SearchCategory,
        modificationDate: DateFilterOption?,
        creationDate: DateFilterOption?,
    ): List<MegaNode> = withContext(ioDispatcher) {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1L),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate
        )
        megaApiGateway.searchWithFilter(
            filter = filter,
            order = sortOrderIntMapper(order),
            megaCancelToken = megaCancelToken,
        )
    }

    override suspend fun getChildren(
        nodeId: NodeId?,
        query: String,
        order: SortOrder,
        searchTarget: SearchTarget,
        searchCategory: SearchCategory,
        modificationDate: DateFilterOption?,
        creationDate: DateFilterOption?,
    ): List<MegaNode> = withContext(ioDispatcher) {
        val megaCancelToken = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            searchQuery = query,
            parentHandle = nodeId ?: NodeId(-1),
            searchTarget = searchTarget,
            searchCategory = searchCategory,
            modificationDate = modificationDate,
            creationDate = creationDate
        )
        megaApiGateway.getChildren(
            filter = filter,
            order = sortOrderIntMapper(order),
            megaCancelToken = megaCancelToken,
        )
    }

    override suspend fun getInShares() = withContext(ioDispatcher) {
        megaApiGateway.getInShares(sortOrderIntMapper(getCloudSortOrder()))
    }

    override suspend fun getOutShares() = withContext(ioDispatcher) {
        val searchNodes = ArrayList<MegaNode>()
        val outShares =
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(getCloudSortOrder()))
        val addedHandles = mutableSetOf<Long>()
        for (outShare in outShares) {
            if (!addedHandles.contains(outShare.nodeHandle)) {
                megaApiGateway.getMegaNodeByHandle(outShare.nodeHandle)?.let {
                    addedHandles.add(it.handle)
                    searchNodes.add(it)
                }
            }
        }
        searchNodes
    }

    override suspend fun getPublicLinks() = withContext(ioDispatcher) {
        megaApiGateway.getPublicLinks(sortOrderIntMapper(getLinksSortOrder()))
    }
}