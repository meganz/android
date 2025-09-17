package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.SortOrderSource
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.api.StreamingGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ChatFilesFolderUserAttributeMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MegaExceptionMapper
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OfflineInformationMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.node.FileNodeMapper
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.mapper.node.NodeListMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.node.NodeShareKeyResultMapper
import mega.privacy.android.data.mapper.node.TypedNodeMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelIntMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionIntMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.SynchronisationException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.extension.Chunk
import mega.privacy.android.domain.extension.ConcurrencyStrategy
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Default implementation of [NodeRepository]
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
 * @property offlineNodeInformationMapper
 * @property offlineInformationMapper
 * @property fileGateway
 * @property chatFilesFolderUserAttributeMapper
 * @property streamingGateway
 */
internal class NodeRepositoryImpl @Inject constructor(
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
    private val nodeListMapper: NodeListMapper,
    private val fileNodeMapper: FileNodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper,
    private val offlineInformationMapper: OfflineInformationMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    private val streamingGateway: StreamingGateway,
    private val nodeUpdateMapper: NodeUpdateMapper,
    private val accessPermissionMapper: AccessPermissionMapper,
    private val nodeShareKeyResultMapper: NodeShareKeyResultMapper,
    private val accessPermissionIntMapper: AccessPermissionIntMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val megaNodeMapper: MegaNodeMapper,
    private val nodeLabelIntMapper: NodeLabelIntMapper,
    private val megaSearchFilterMapper: MegaSearchFilterMapper,
    private val cancelTokenProvider: CancelTokenProvider,
    private val workManagerGateway: WorkManagerGateway,
    private val stringListMapper: StringListMapper,
    private val nodeLabelMapper: NodeLabelMapper,
    private val typedNodeMapper: TypedNodeMapper,
) : NodeRepository {

    override suspend fun getNodeOutgoingShares(nodeId: NodeId) =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { megaNode ->
                megaApiGateway.getOutShares(megaNode).map { shareDataMapper(it) }
            } ?: emptyList()
        }

    override suspend fun getUnverifiedIncomingShares(order: SortOrder) =
        withContext(ioDispatcher) {
            megaApiGateway.getUnverifiedIncomingShares(sortOrderIntMapper(order)).map {
                shareDataMapper(it)
            }
        }

    override suspend fun getUnverifiedOutgoingShares(order: SortOrder) =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(order))
                .filter { !it.isVerified }
                .map { shareDataMapper(it) }
        }

    override suspend fun getAllOutgoingShares(
        order: SortOrder,
    ) = withContext(ioDispatcher) {
        megaApiGateway.getOutgoingSharesNode(
            sortOrderIntMapper(
                sortOrder = order,
                source = SortOrderSource.OutgoingShares
            )
        )
            .filter { it.user != null }
            .let { outgoingShares ->
                val verifiedShares = outgoingShares.filter { it.isVerified }
                val shareCount = verifiedShares
                    .groupBy { it.nodeHandle }
                    .mapValues { it.value.size }
                // Set count to 0, so that UI can show unverified icon based on it
                outgoingShares
                    .distinctBy { it.nodeHandle }
                    .filter { it.isVerified || isValidNode(NodeId(it.nodeHandle)) }
                    .map {
                        shareDataMapper(
                            share = it,
                            shareCount = if (it.isVerified) {
                                shareCount.getOrDefault(it.nodeHandle, 1)
                            } else {
                                0
                            }
                        )
                    }
            }
    }

    override suspend fun getAllIncomingShares(
        order: SortOrder,
    ) = withContext(ioDispatcher) {
        val (unverifiedShares, verifiedShares) = awaitAll(
            async { megaApiGateway.getUnverifiedIncomingShares(sortOrderIntMapper(order)) },
            async { megaApiGateway.getVerifiedIncomingShares(sortOrderIntMapper(order)) }
        )
        // Set count to 0, so that UI can use it as a flag
        val unverifiedSharesMapped = unverifiedShares
            .filter { isValidNode(NodeId(it.nodeHandle)) && it.user != null }
            .map { shareDataMapper(it, 0) }

        unverifiedSharesMapped + verifiedShares
            .filter { it.user != null }
            .map { shareDataMapper(it, 1) }
    }

    override suspend fun isNodeInRubbishBin(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { megaApiGateway.isInRubbish(it) }
            ?: run {
                Timber.w("isNodeInRubbishBin returns false because the node with handle $nodeId was not found")
                false
            }
    }

    override suspend fun isNodeInBackups(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInBackups(it) }
            ?: run {
                Timber.w("isNodeInBackups returns false because the node with handle $handle was not found")
                false
            }
    }

    override suspend fun isNodeInCloudDrive(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInCloudDrive(it) }
            ?: run {
                Timber.w("isNodeInCloudDrive returns false because the node with handle $handle was not found")
                false
            }
    }

    override suspend fun getBackupFolderId(): NodeId =
        withContext(ioDispatcher) {
            val backupsFolderAttributeIdentifier = MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getUserAttribute(
                    backupsFolderAttributeIdentifier,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (request.paramType == backupsFolderAttributeIdentifier) {
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                                } else {
                                    continuation.failWithError(error, "getBackupFolderId")
                                }
                            }
                        }
                    ))
            }
        }

    override suspend fun getNodeById(nodeId: NodeId) = withContext(ioDispatcher) {
        val offline = getOfflineNode(nodeId.longValue)
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            convertToUnTypedNode(node = it, offline = offline)
        }
    }

    override suspend fun getParentNodeId(nodeId: NodeId): NodeId? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            NodeId(it.parentHandle)
        }
    }

    override suspend fun doesNodeExist(nodeId: NodeId): Boolean = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue) != null
    }

    override suspend fun getNodeFromSerializedData(serializedData: String) =
        withContext(ioDispatcher) {
            megaApiGateway.unSerializeNode(serializedData)?.let {
                nodeMapper.invoke(megaNode = it, requireSerializedData = true)
            }
        }

    override suspend fun getNodePathById(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getNodePathByHandle(nodeId.longValue) ?: ""
    }

    @Deprecated("Use getTypedNodesById")
    override suspend fun getNodeChildren(
        nodeId: NodeId,
        order: SortOrder?,
    ): List<UnTypedNode> = withContext(ioDispatcher) {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            parentHandle = nodeId,
        )
        val offlineItems = async { getAllOfflineNodeHandle() }
        val childList = async {
            megaApiGateway.getChildren(
                filter,
                sortOrderIntMapper(order ?: SortOrder.ORDER_NONE),
                token
            )
        }
        mapMegaNodesToUnTypedNodes(childList.await(), offlineItems.await())
    }

    private suspend fun mapMegaNodesToUnTypedNodes(
        childList: List<MegaNode>,
        offlineItems: Map<String, Offline>?,
    ): List<UnTypedNode> = coroutineScope {
        childList.map { megaNode ->
            async {
                convertToUnTypedNode(
                    node = megaNode,
                    offline = offlineItems?.get(megaNode.handle.toString())
                )
            }
        }.awaitAll()
    }

    override suspend fun getNodeChildrenFileTypes(
        nodeId: NodeId,
        order: SortOrder?,
    ): List<FileTypeInfo> = withContext(ioDispatcher) {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            parentHandle = nodeId,
        )
        val childList = megaApiGateway.getChildren(
            filter,
            sortOrderIntMapper(order ?: SortOrder.ORDER_NONE),
            token
        )
        childList.map { megaNode ->
            fileTypeInfoMapper(megaNode.name, megaNode.duration)
        }
    }

    override suspend fun getNodeHistoryVersions(handle: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle.longValue)?.let { megaNode ->
            megaApiGateway.getVersions(megaNode).map { version ->
                convertToUnTypedNode(node = version, offline = getOfflineNode(version.handle))
            }
        } ?: throw SynchronisationException("Non null node found be null when fetched from api")
    }

    override suspend fun getFolderTreeInfo(folderNode: TypedFolderNode): FolderTreeInfo =
        withContext(ioDispatcher) {
            if (folderNode is PublicLinkFolder) {
                getPublicLinkFolderTreeInfo(megaNodeMapper(folderNode))
            } else {
                getFolderTreeInfo(megaNodeMapper(folderNode))
            }
        }

    override suspend fun getFolderTreeInfo(folderNode: FolderNode): FolderTreeInfo =
        withContext(ioDispatcher) {
            getFolderTreeInfo(megaApiGateway.getMegaNodeByHandle(folderNode.id.longValue))
        }

    private suspend fun getFolderTreeInfo(megaNode: MegaNode?) =
        suspendCancellableCoroutine { continuation ->
            megaApiGateway.getFolderInfo(
                megaNode,
                continuation.getRequestListener("getFolderTreeInfo") {
                    with(it.megaFolderInfo) {
                        FolderTreeInfo(
                            numberOfFiles = numFiles,
                            numberOfFolders = numFolders,
                            totalCurrentSizeInBytes = currentSize,
                            numberOfVersions = numVersions,
                            sizeOfPreviousVersionsInBytes = versionsSize,
                        )
                    }
                }
            )
        }

    private suspend fun getPublicLinkFolderTreeInfo(megaNode: MegaNode?) =
        suspendCancellableCoroutine { continuation ->
            megaApiFolderGateway.getFolderInfo(
                megaNode,
                continuation.getRequestListener("getFolderTreeInfo") {
                    with(it.megaFolderInfo) {
                        FolderTreeInfo(
                            numberOfFiles = numFiles,
                            numberOfFolders = numFolders,
                            totalCurrentSizeInBytes = currentSize,
                            numberOfVersions = numVersions,
                            sizeOfPreviousVersionsInBytes = versionsSize,
                        )
                    }
                }
            )
        }

    override suspend fun deleteNodeVersionByHandle(nodeVersionToDelete: NodeId): Unit =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeVersionToDelete.longValue)?.let { version ->
                suspendCancellableCoroutine { continuation ->
                    megaApiGateway.deleteVersion(
                        version,
                        continuation.getRequestListener("deleteNodeVersionByHandle") {})
                }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }

    override fun monitorNodeUpdates(): Flow<NodeUpdate> {
        return megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull {
                it.nodeList?.map { megaNode ->
                    convertToUnTypedNode(megaNode) to nodeUpdateMapper(megaNode)
                }
            }
            .map { nodes ->
                NodeUpdate(nodes.toMap())
            }
            .flowOn(ioDispatcher)
    }

    override fun monitorOfflineNodeUpdates(): Flow<List<Offline>> =
        megaLocalRoomGateway.monitorOfflineUpdates()

    override suspend fun isNodeInRubbishOrDeleted(nodeHandle: Long): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeHandle)?.let { megaApiGateway.isInRubbish(it) }
                ?: true
        }

    override suspend fun getOfflineNodeInformation(nodeId: NodeId) =
        withContext(ioDispatcher) {
            getOfflineNode(nodeId.longValue)
                ?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun saveOfflineNodeInformation(
        offlineNodeInformation: OfflineNodeInformation,
        parentOfflineInformationId: Long?,
    ) = withContext(ioDispatcher) {
        megaLocalRoomGateway.saveOfflineInformation(
            offlineInformationMapper(offlineNodeInformation, parentOfflineInformationId?.toInt())
        )
    }

    override suspend fun convertBase64ToHandle(base64: String): Long = withContext(ioDispatcher) {
        megaApiGateway.base64ToHandle(base64)
    }

    override suspend fun getOfflineNodeInformation(nodeHandle: Long) =
        withContext(ioDispatcher) {
            getOfflineNode(nodeHandle)
                ?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun startOfflineSyncWorker() = withContext(ioDispatcher) {
        workManagerGateway.startOfflineSync()
    }

    override suspend fun getOwnerIdFromInShare(nodeId: NodeId, recursive: Boolean): UserId? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
                megaApiGateway.getUserFromInShare(node, recursive)?.let { user ->
                    UserId(user.handle)
                }
            }
        }

    override suspend fun getNodeAccessPermission(nodeId: NodeId): AccessPermission? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { node ->
                accessPermissionMapper(megaApiGateway.getAccess(node))
            }
        }

    private suspend fun convertToUnTypedNode(
        node: MegaNode,
        offline: Offline? = null,
    ): UnTypedNode {
        return nodeMapper(
            megaNode = node, offline = offline,
        )
    }


    override suspend fun stopSharingNode(nodeId: NodeId): Unit = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            megaApiGateway.stopSharingNode(it)
        }
    }

    override suspend fun createShareKey(node: FolderNode): (suspend (AccessPermission, String) -> Unit)? {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(node.id.longValue)?.let { megaNode ->
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("openShareDialog") {
                        return@getRequestListener nodeShareKeyResultMapper(megaNode)
                    }

                    megaApiGateway.openShareDialog(megaNode, listener)
                }
            }
        }
    }

    override suspend fun setShareAccess(
        nodeId: NodeId,
        accessPermission: AccessPermission,
        email: String,
    ) {
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                nodeShareKeyResultMapper(it)(accessPermission, email)
            }
        }
    }

    override suspend fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

    override suspend fun isValidNode(nodeId: NodeId) = withContext(ioDispatcher) {
        nodeId.longValue != getInvalidHandle() && megaApiGateway.getMegaNodeByHandle(nodeId.longValue) != null
    }

    override suspend fun getRootNode() = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()?.let {
            convertToUnTypedNode(it)
        }
    }

    override suspend fun removedInSharedNodesByEmail(email: String): Unit =
        withContext(ioDispatcher) {
            runCatching {
                megaApiGateway.getContact(email)?.let {
                    megaApiGateway.getInShares(it).forEach { node ->
                        megaApiGateway.deleteNode(node, null)
                    }
                }
            }
        }

    override suspend fun getInShares(email: String): List<UnTypedNode> = withContext(ioDispatcher) {
        runCatching {
            megaApiGateway.getContact(email)?.let {
                megaApiGateway.getInShares(it).map { node ->
                    convertToUnTypedNode(node)
                }
            }
        }.getOrNull() ?: emptyList()
    }

    override suspend fun getDefaultNodeHandle(folderName: String) = withContext(ioDispatcher) {
        megaApiGateway.getNodeByPath(folderName, megaApiGateway.getRootNode())
            ?.takeIf { it.isFolder }?.let { NodeId(it.handle) }
    }

    override suspend fun checkNodeCanBeMovedToTargetNode(
        nodeId: NodeId,
        targetNodeId: NodeId,
    ): Boolean = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
        val targetNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)

        if (node != null && targetNode != null) {
            megaApiGateway.checkMoveErrorExtended(
                node,
                targetNode
            ).errorCode != MegaError.API_OK
        } else {
            false
        }
    }

    override suspend fun copyNode(
        nodeToCopy: NodeId,
        nodeToCopySerializedData: String?,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = getMegaNodeByHandle(nodeToCopy, true)
            ?: nodeToCopySerializedData?.let { MegaNode.unserialize(it) }
        val parent = getMegaNodeByHandle(newNodeParent, true)
        requireNotNull(node) { "Node to copy with handle $nodeToCopy not found" }
        requireNotNull(parent) { "Destination node with handle $newNodeParent not found" }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("copyNode") { NodeId(it.nodeHandle) }
            megaApiGateway.copyNode(
                nodeToCopy = node,
                newNodeParent = parent,
                newNodeName = newNodeName,
                listener = listener
            )
        }
    }

    override suspend fun copyNode(
        nodeToCopy: TypedNode,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = megaNodeMapper(nodeToCopy)
        val parent = getMegaNodeByHandle(newNodeParent, true)
        requireNotNull(node) { "Node to copy $nodeToCopy not found" }
        requireNotNull(parent) { "Destination node with handle $newNodeParent not found" }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("copyNode") { NodeId(it.nodeHandle) }
            megaApiGateway.copyNode(
                nodeToCopy = node,
                newNodeParent = parent,
                newNodeName = newNodeName,
                listener = listener
            )
        }
    }

    override suspend fun copyPublicNode(
        publicNodeToCopy: Node,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = MegaNode.unserialize(publicNodeToCopy.serializedData)
        val parent = getMegaNodeByHandle(newNodeParent, true)
        requireNotNull(node) { "Node to copy with handle $publicNodeToCopy not found" }
        requireNotNull(parent) { "Destination node with handle $newNodeParent not found" }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("copyNode") { NodeId(it.nodeHandle) }
            megaApiGateway.copyNode(
                nodeToCopy = node,
                newNodeParent = parent,
                newNodeName = newNodeName,
                listener = listener
            )
        }
    }


    override suspend fun moveNode(
        nodeToMove: NodeId,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = getMegaNodeByHandle(nodeToMove, true)
        val parent = getMegaNodeByHandle(newNodeParent, true)
        requireNotNull(node) { "Node to move with handle $nodeToMove not found" }
        requireNotNull(parent) { "Destination node with handle $newNodeParent not found" }
        return@withContext moveNode(node, parent, newNodeName)
    }

    private suspend fun moveNode(
        node: MegaNode,
        parent: MegaNode,
        newNodeName: String?,
    ): NodeId {
        val result = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    continuation.resumeWith(Result.success(request to error))
                }
            )
            megaApiGateway.moveNode(
                nodeToMove = node,
                newNodeParent = parent,
                newNodeName = newNodeName,
                listener = listener
            )
        }
        return when {
            result.second.errorCode == MegaError.API_OK -> NodeId(result.first.nodeHandle)
            result.second.errorCode == MegaError.API_EOVERQUOTA
                    && megaApiGateway.isForeignNode(parent.handle) -> throw ForeignNodeException()

            else -> throw result.second.toException("moveNode")
        }
    }

    override suspend fun getFingerprint(filePath: String) = withContext(ioDispatcher) {
        megaApiGateway.getFingerprint(filePath)
    }

    override suspend fun getParentNode(nodeId: NodeId) = withContext(ioDispatcher) {
        val megaNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
        megaNode?.let {
            megaApiGateway.getParentNode(megaNode)
                ?.let { nodeMapper(megaNode = it, offline = getOfflineNode(nodeId.longValue)) }
        }
    }

    override suspend fun getRootParentNode(nodeId: NodeId): UnTypedNode? =
        withContext(ioDispatcher) {
            var currentRootParent = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
            while (currentRootParent != null) {
                megaApiGateway.getParentNode(currentRootParent)?.let {
                    currentRootParent = it
                } ?: break
            }
            currentRootParent?.let {
                nodeMapper(
                    megaNode = it,
                    offline = getOfflineNode(nodeId.longValue)
                )
            }
        }

    override suspend fun getNodesByOriginalFingerprint(
        originalFingerprint: String,
        parentNodeId: NodeId?,
    ): List<UnTypedNode> =
        withContext(ioDispatcher) {
            val parentNode =
                parentNodeId?.let { megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue) }
            megaApiGateway.getNodesByOriginalFingerprint(
                originalFingerprint = originalFingerprint,
                parentNode = parentNode,
            )?.let { megaNodeList -> nodeListMapper(megaNodeList) }.orEmpty()
        }

    override suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNodeId: NodeId,
    ): UnTypedNode? = withContext(ioDispatcher) {
        val megaNode = megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
        megaApiGateway.getNodeByFingerprintAndParentNode(fingerprint, megaNode)
            ?.let { nodeMapper(megaNode = it, offline = getOfflineNode(it.handle)) }.also {
                Timber.d("Found node by fingerprint with the same local fingerprint in node with handle: ${parentNodeId}, node: $it")
            }
    }

    override suspend fun getNodeByFingerprint(fingerprint: String) =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByFingerprint(fingerprint)
                ?.let { nodeMapper(megaNode = it, offline = getOfflineNode(it.handle)) }
        }.also {
            Timber.d("Found node by fingerprint with the same local fingerprint in the account, node: $it")
        }

    override suspend fun getNodeGPSCoordinates(nodeId: NodeId): Pair<Double, Double> =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                Pair(it.latitude, it.longitude)
            } ?: Pair(0.0, 0.0)
        }

    override suspend fun getChildNode(parentNodeId: NodeId?, name: String?) =
        withContext(ioDispatcher) {
            val parent = parentNodeId
                ?.let { megaApiGateway.getMegaNodeByHandle(it.longValue) }
            megaApiGateway.getChildNode(parent, name)
                ?.let { nodeMapper(megaNode = it, offline = getOfflineNode(it.handle)) }
        }

    override suspend fun setOriginalFingerprint(nodeId: NodeId, originalFingerprint: String) =
        withContext(ioDispatcher) {
            val node = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
            requireNotNull(node) { "Node not found" }
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("setOriginalFingerprint") {}
                megaApiGateway.setOriginalFingerprint(
                    node = node,
                    originalFingerprint = originalFingerprint,
                    listener = listener
                )
            }
        }

    private suspend fun getMegaNodeByHandle(nodeId: NodeId, attemptFromFolderApi: Boolean = false) =
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
            ?: takeIf { attemptFromFolderApi }
                ?.let { megaApiFolderGateway.getMegaNodeByHandle(nodeId.longValue) }
                ?.let { megaApiFolderGateway.authorizeNode(it) }

    override suspend fun getNodeByHandle(handle: Long, attemptFromFolderApi: Boolean) =
        withContext(ioDispatcher) {
            getMegaNodeByHandle(NodeId(handle), attemptFromFolderApi)
                ?.let { nodeMapper(megaNode = it, offline = getOfflineNode(it.handle)) }
        }

    override suspend fun getNodeFromChatMessage(chatId: Long, messageId: Long, messageIndex: Int) =
        withContext(ioDispatcher) {
            (megaChatApiGateway.getMessage(chatId, messageId)
                ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
                ?.let { message ->
                    getChatFile(
                        megaNode = message.megaNodeList.get(messageIndex),
                        chat = megaChatApiGateway.getChatRoom(chatId)
                    )
                }
        }

    override suspend fun getNodesFromChatMessage(chatId: Long, messageId: Long) =
        withContext(ioDispatcher) {
            (megaChatApiGateway.getMessage(chatId, messageId)
                ?: megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
                ?.let { message ->
                    val nodes = message.megaNodeList
                    val chat = megaChatApiGateway.getChatRoom(chatId)
                    (0 until nodes.size()).mapNotNull { index ->
                        nodes.get(index)?.let {
                            getChatFile(megaNode = it, chat = chat)
                        }
                    }
                } ?: emptyList()
        }

    private suspend fun getChatFile(megaNode: MegaNode, chat: MegaChatRoom?): FileNode? {
        return if (chat?.isPreview == true) {
            megaApiGateway.authorizeChatNode(megaNode, chat.authorizationToken)
        } else {
            megaNode
        }?.let {
            fileNodeMapper(
                megaNode = it,
                requireSerializedData = false,
                offline = null
            )
        }
    }

    override suspend fun getRubbishNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getRubbishBinNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getBackupsNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getBackupsNode()?.let { megaNode ->
                convertToUnTypedNode(node = megaNode, offline = getOfflineNode(megaNode.handle))
            }
        }

    override suspend fun getRootNodeFromMegaApiFolder(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getRootNode()?.let { megaNode ->
                convertToUnTypedNode(node = megaNode, offline = getOfflineNode(megaNode.handle))
            }
        }

    override suspend fun getParentNodeFromMegaApiFolder(parentHandle: Long): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(parentHandle)?.let { megaNode ->
                convertToUnTypedNode(node = megaNode, offline = getOfflineNode(megaNode.handle))
            }
        }

    @Throws(IllegalArgumentException::class)
    override suspend fun deleteNodeByHandle(nodeToDelete: NodeId) = withContext(ioDispatcher) {
        val node = getMegaNodeByHandle(nodeToDelete, true)
            ?: throw IllegalArgumentException("Node to delete with handle $nodeToDelete not found")
        if (!megaApiGateway.isInRubbish(node)) {
            throw IllegalArgumentException("Node needs to be in the rubbish bin before deleting it")
        }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("deleteNodeByHandle") {}
            megaApiGateway.deleteNode(
                node = node,
                listener = listener
            )
        }
    }

    override suspend fun exportNode(nodeToExport: NodeId, expireTime: Long?): String =
        withContext(ioDispatcher) {
            val node = getMegaNodeByHandle(nodeToExport, true)
            exportNode(node, expireTime)
        }

    override suspend fun exportNode(node: TypedNode) = withContext(ioDispatcher) {
        exportNode(megaNodeMapper(node), null)
    }

    private suspend fun exportNode(node: MegaNode?, expireTime: Long?): String {
        requireNotNull(node) { "Node to export not found" }
        require(!node.isTakenDown) { "Node to export is taken down" }

        if (node.isExported && !node.isExpired && node.expirationTime == expireTime) {
            return node.publicLink.orEmpty()
        }
        return suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("exportNode") { it.link.orEmpty() }
            megaApiGateway.exportNode(node, expireTime, listener)
        }
    }

    override suspend fun disableExport(nodeToDisable: NodeId) = withContext(ioDispatcher) {
        val node = getMegaNodeByHandle(nodeToDisable, true)
        requireNotNull(node) { "Node to disable export with handle ${nodeToDisable.longValue} not found" }

        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("disableExport") { }
            megaApiGateway.disableExport(node, listener)
        }
    }

    override suspend fun setNodeCoordinates(nodeId: NodeId, latitude: Double, longitude: Double) =
        withContext(ioDispatcher) {
            val node = getMegaNodeByHandle(nodeId, true)
            requireNotNull(node) { "Node to disable export with handle ${nodeId.longValue} not found" }
            megaApiGateway.setNodeCoordinates(node, latitude, longitude)
        }

    override suspend fun getIncomingShareParentUserEmail(nodeId: NodeId) =
        withContext(ioDispatcher) {
            return@withContext megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                megaApiGateway.getUserFromInShare(it, true)?.email
            }
        }

    override suspend fun checkIfNodeHasTheRequiredAccessLevelPermission(
        nodeId: NodeId,
        level: AccessPermission,
    ): Boolean = withContext(ioDispatcher) {
        val accessLevel = accessPermissionIntMapper(level)
        getMegaNodeByHandle(nodeId, true)?.let {
            megaApiGateway.checkAccessErrorExtended(it, accessLevel).errorCode == MegaError.API_OK
        } ?: false
    }

    override suspend fun getAllOfflineNodes(): List<OfflineNodeInformation> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getAllOfflineInfo().map {
                offlineNodeInformationMapper(it)
            }
        }

    override suspend fun getOfflineNodesByParentId(parentId: Int): List<OfflineNodeInformation> =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getOfflineInfoByParentId(parentId).map {
                offlineNodeInformationMapper(it)
            }
        }

    override suspend fun getOfflineFolderInfo(parentId: Int): OfflineFolderInfo =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getOfflineInfoByParentId(parentId).let { offlineNodes ->
                val numberOfFolders = offlineNodes.count { it.isFolder }
                OfflineFolderInfo(numberOfFolders, offlineNodes.size - numberOfFolders)
            }
        }

    override suspend fun getOfflineNodeById(id: Int) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getOfflineLineById(id)?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun removeOfflineNodeById(id: Int) {
        withContext(ioDispatcher) { megaLocalRoomGateway.removeOfflineInformationById(id) }
    }

    override suspend fun removeOfflineNodeByIds(ids: List<Int>) {
        withContext(ioDispatcher) {
            megaLocalRoomGateway.removeOfflineInformationByIds(ids)
        }
    }

    override fun getNodeLabel(label: Int): NodeLabel? = nodeLabelMapper(label)

    override suspend fun setNodeLabel(nodeId: NodeId, label: NodeLabel): Unit =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                megaApiGateway.setNodeLabel(it, nodeLabelIntMapper(label))
            }
        }

    override suspend fun resetNodeLabel(nodeId: NodeId): Unit =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                megaApiGateway.resetNodeLabel(it)
            }
        }

    override suspend fun updateFavoriteNode(nodeId: NodeId, isFavorite: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.setNodeFavourite(
                node = megaApiGateway.getMegaNodeByHandle(nodeId.longValue),
                favourite = isFavorite
            )
        }

    override suspend fun updateNodeSensitive(nodeId: NodeId, isSensitive: Boolean) =
        withContext(ioDispatcher) {
            megaApiGateway.setNodeSensitive(
                node = megaApiGateway.getMegaNodeByHandle(nodeId.longValue),
                sensitive = isSensitive,
            )
        }

    override suspend fun clearOffline() =
        withContext(ioDispatcher) { megaLocalRoomGateway.clearOffline() }

    override suspend fun moveNodeToRubbishBinByHandle(nodeId: NodeId) {
        withContext(ioDispatcher) {
            val rubbish = megaApiGateway.getRubbishBinNode()
            val node = getMegaNodeByHandle(nodeId, true)
            requireNotNull(node) { "Node to move with handle $node not found" }
            requireNotNull(rubbish) { "Rubbish bin node not found" }
            moveNode(node, rubbish, null)
        }
    }

    override fun getNodeLabelList(): List<NodeLabel> =
        buildList {
            add(NodeLabel.RED)
            add(NodeLabel.ORANGE)
            add(NodeLabel.YELLOW)
            add(NodeLabel.GREEN)
            add(NodeLabel.BLUE)
            add(NodeLabel.PURPLE)
            add(NodeLabel.GREY)
        }

    private suspend fun getAllOfflineNodeHandle() =
        megaLocalRoomGateway.getAllOfflineInfo().associateBy { it.handle }

    private suspend fun getOfflineNode(handle: Long) =
        megaLocalRoomGateway.getOfflineInformation(handle)

    override suspend fun getContactVerificationEnabledWarning() = withContext(ioDispatcher) {
        megaApiGateway.getContactVerificationWarningEnabled()
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun leaveShareByHandle(nodeToLeaveShare: NodeId) = withContext(ioDispatcher) {
        val node = getMegaNodeByHandle(nodeToLeaveShare, true)
            ?: throw IllegalArgumentException("Node to delete with handle $nodeToLeaveShare not found")
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("deleteNodeByHandle") {}
            megaApiGateway.deleteNode(
                node = node,
                listener = listener
            )
        }
    }

    override suspend fun shareFolder(
        nodeId: NodeId,
        email: String,
        accessPermission: AccessPermission,
    ) {
        withContext(ioDispatcher) {
            val node = getMegaNodeByHandle(nodeId, true)
                ?: throw IllegalArgumentException("Node to share with handle $nodeId not found")
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("shareFolder") {}
                megaApiGateway.setShareAccess(
                    megaNode = node,
                    accessLevel = accessPermissionIntMapper(accessPermission),
                    email = email,
                    listener = listener
                )
            }
        }
    }

    override suspend fun getMyUserHandleBinary(): Long {
        return withContext(ioDispatcher) {
            megaApiGateway.getMyUserHandleBinary()
        }
    }

    override suspend fun getNodesByFingerprint(fingerprint: String): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            megaApiGateway.getNodesByFingerprint(fingerprint).map {
                convertToUnTypedNode(it)
            }
        }
    }

    override suspend fun getOwnerNodeHandle(nodeId: NodeId): Long? {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.owner
        }
    }

    override suspend fun getLocalLink(node: TypedNode): String? = withContext(ioDispatcher) {
        runCatching {
            megaNodeMapper(node)?.let { node ->
                megaApiGateway.httpServerGetLocalLink(node)
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
    }

    override suspend fun createFolder(name: String, parentNodeId: NodeId?) =
        withContext(ioDispatcher) {
            val parentMegaNode = when (parentNodeId) {
                null -> megaApiGateway.getRootNode()
                else -> megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
            }

            requireNotNull(parentMegaNode) { "Parent node not found" }

            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("createFolder") {
                    NodeId(it.nodeHandle)
                }
                megaApiGateway.createFolder(name, parentMegaNode, listener)
            }
        }

    override suspend fun isEmptyFolder(node: TypedNode): Boolean = withContext(ioDispatcher) {
        when (node) {
            is FileNode -> false
            is FolderNode -> megaApiGateway.getMegaNodeByHandle(node.id.longValue)
                ?.let { isChildrenEmpty(it) } ?: true

            else -> true
        }
    }

    private suspend fun isChildrenEmpty(parent: MegaNode): Boolean {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(
            parentHandle = NodeId(parent.handle),
        )
        megaApiGateway.getChildren(filter, sortOrderIntMapper(SortOrder.ORDER_NONE), token).let {
            if (it.isNotEmpty()) {
                it.forEach { childNode ->
                    if (childNode.isFolder.not() || isChildrenEmpty(childNode).not()) {
                        return false
                    }
                }
            }
            return true
        }
    }

    override suspend fun setNodeDescription(nodeHandle: NodeId, description: String?) =
        withContext(ioDispatcher) {
            val node = megaApiGateway.getMegaNodeByHandle(nodeHandle.longValue)
            requireNotNull(node) { "Node not found" }
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("setNodeDescription") {}
                megaApiGateway.setNodeDescription(node, description, listener)
            }
        }

    override suspend fun getOfflineNodesByQuery(
        query: String,
        parentId: Int,
    ): List<OfflineNodeInformation> = withContext(ioDispatcher) {
        // Database fields are encrypted, so we need to filter by name in memory
        if (parentId == -1) {
            megaLocalRoomGateway.getAllOfflineInfo()
        } else {
            megaLocalRoomGateway.getOfflineInfoByParentId(parentId)
        }.filter {
            it.name.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))
        }.map {
            offlineNodeInformationMapper(it)
        }
    }

    override suspend fun addNodeTag(nodeHandle: NodeId, tag: String) = withContext(ioDispatcher) {
        val node = megaApiGateway.getMegaNodeByHandle(nodeHandle.longValue)
        requireNotNull(node) { "Node not found" }
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("addNodeTag") {}
            megaApiGateway.addNodeTag(node, tag, listener)
        }
    }

    override suspend fun removeNodeTag(nodeHandle: NodeId, tag: String) =
        withContext(ioDispatcher) {
            val node = megaApiGateway.getMegaNodeByHandle(nodeHandle.longValue)
            requireNotNull(node) { "Node not found" }
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("removeNodeTag") {}
                megaApiGateway.removeNodeTag(node, tag, listener)
            }
        }

    override suspend fun updateNodeTag(nodeHandle: NodeId, oldTag: String, newTag: String) =
        withContext(ioDispatcher) {
            val node = megaApiGateway.getMegaNodeByHandle(nodeHandle.longValue)
            requireNotNull(node) { "Node not found" }
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("updateNodeTag") {}
                megaApiGateway.updateNodeTag(node, oldTag, newTag, listener)
            }
        }

    override suspend fun hasSensitiveDescendant(nodeId: NodeId): Boolean =
        withContext(ioDispatcher) {
            val token = cancelTokenProvider.getOrCreateCancelToken()
            val filter = megaSearchFilterMapper(
                parentHandle = nodeId,
                sensitivityFilter = SensitivityFilterOption.SensitiveOnly,
            )
            megaApiGateway.searchWithFilter(
                filter,
                sortOrderIntMapper(SortOrder.ORDER_NONE),
                token,
            ).isNotEmpty()
        }

    override suspend fun hasSensitiveInherited(nodeId: NodeId): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
                megaApiGateway.isSensitiveInherited(it)
            } ?: false
        }

    override suspend fun getAllNodeTags(searchString: String): List<String>? =
        withContext(ioDispatcher) {
            val token = cancelTokenProvider.getOrCreateCancelToken()
            megaApiGateway.getAllNodeTags(searchString, token)?.let {
                stringListMapper(it)
            }
        }

    override suspend fun moveOrRemoveDeconfiguredBackupNodes(
        deconfiguredBackupRoot: NodeId,
        backupDestination: NodeId,
    ): NodeId = withContext(ioDispatcher) {
        val result = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    continuation.resumeWith(Result.success(request to error))
                }
            )
            megaApiGateway.moveOrRemoveDeconfiguredBackupNodes(
                deconfiguredBackupRoot, backupDestination, listener
            )
        }
        return@withContext when {
            result.second.errorCode == MegaError.API_OK -> NodeId(result.first.nodeHandle)
            else -> throw result.second.toException("moveOrRemoveDeconfiguredBackupNodes")
        }
    }

    override suspend fun isNodeSynced(nodeId: NodeId): Boolean = withContext(ioDispatcher) {
        val syncs = megaApiGateway.getSyncs()
        for (i in 0..syncs.size()) {
            syncs.get(i)?.let { syncNode ->
                if (syncNode.megaHandle == nodeId.longValue) {
                    return@withContext true
                }
            }
        }
        return@withContext false
    }

    override suspend fun getAllSyncedNodeIds(): Set<NodeId> = withContext(ioDispatcher) {
        val syncs = megaApiGateway.getSyncs()
        val syncedNodeIds = mutableSetOf<NodeId>()
        for (i in 0..syncs.size()) {
            syncs.get(i)?.let { syncNode ->
                syncedNodeIds.add(NodeId(syncNode.megaHandle))
            }
        }
        syncedNodeIds
    }

    override suspend fun removeAllVersions() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("removeAllVersions") {}
            megaApiGateway.removeVersions(listener)
        }
    }

    override suspend fun cleanRubbishBin() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("cleanRubbishBin") {}
            megaApiGateway.cleanRubbishBin(listener)
        }
    }

    override suspend fun getRootNodeId(): NodeId? = withContext(ioDispatcher) {
        megaApiGateway.getRootNode()?.let { NodeId(it.handle) }
    }

    override suspend fun getNodeNameById(nodeId: NodeId): String? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.name
    }

    override suspend fun getTypedNodesById(
        nodeId: NodeId,
        order: SortOrder?,
        folderTypeData: FolderTypeData?,
    ): List<TypedNode> = withContext(ioDispatcher) {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(parentHandle = nodeId)
        val offlineItemsDiffer = async { getAllOfflineNodeHandle() }
        val megaNodesDiffer = async {
            megaApiGateway.getChildren(
                filter,
                sortOrderIntMapper(order ?: SortOrder.ORDER_NONE),
                token
            )
        }
        val offlineItems = offlineItemsDiffer.await()
        val megaNodes = megaNodesDiffer.await()

        megaNodes.mapAsync(getConcurrencyStrategy(megaNodes.size)) { megaNode ->
            typedNodeMapper(
                megaNode = megaNode,
                folderTypeData = folderTypeData,
                offline = offlineItems[megaNode.handle.toString()]
            )
        }
    }

    override suspend fun getTypedNodesByIdInChunks(
        nodeId: NodeId,
        order: SortOrder?,
        initialBatchSize: Int,
        folderTypeData: FolderTypeData?,
    ): Flow<Pair<List<TypedNode>, Boolean>> = flow {
        val token = cancelTokenProvider.getOrCreateCancelToken()
        val filter = megaSearchFilterMapper(parentHandle = nodeId)
        val offlineItems = getAllOfflineNodeHandle()
        val allChildren = megaApiGateway.getChildren(
            filter,
            sortOrderIntMapper(order ?: SortOrder.ORDER_NONE),
            token
        )

        // Emit initial batch immediately
        val initialMegaNodes = allChildren.take(initialBatchSize)
        val initialTypedNodes = initialMegaNodes
            .mapAsync(getConcurrencyStrategy(initialMegaNodes.size)) { megaNode ->
                typedNodeMapper(
                    megaNode = megaNode,
                    folderTypeData = folderTypeData,
                    offline = offlineItems[megaNode.handle.toString()]
                )
            }
        emit(initialTypedNodes to (allChildren.size > initialBatchSize))

        // If there are more nodes, process them and emit the complete list
        if (allChildren.size > initialBatchSize) {
            // Process remaining nodes in chunks
            val remainingMegaNodes = allChildren.drop(initialBatchSize)
            val remainingTypedNodes = remainingMegaNodes
                .mapAsync(getConcurrencyStrategy(remainingMegaNodes.size)) { megaNode ->
                    typedNodeMapper(
                        megaNode = megaNode,
                        folderTypeData = folderTypeData,
                        offline = offlineItems[megaNode.handle.toString()]
                    )
                }
            // Second emit: Complete list (initial + remaining) with hasMore = false
            emit(initialTypedNodes + remainingTypedNodes to false)
        }
    }.flowOn(ioDispatcher)

    /**
     * Determine concurrency strategy based on the number of nodes to map
     * @param count
     * @return ConcurrencyStrategy
     */
    private fun getConcurrencyStrategy(count: Int) = when {
        count <= 100 -> ConcurrencyStrategy.Parallel // Small folders, process in parallel
        count <= 1000 -> ConcurrencyStrategy.ChunkedParallel(Chunk.Count(20))
        count <= 5000 -> ConcurrencyStrategy.ChunkedParallel(Chunk.Count(30))
        else -> ConcurrencyStrategy.ChunkedParallel(Chunk.Count(40)) // Very large folders, larger chunks
    }
}
