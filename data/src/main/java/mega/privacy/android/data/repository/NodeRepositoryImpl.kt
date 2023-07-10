package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheFolderGateway
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
import mega.privacy.android.data.mapper.NodeUpdateMapper
import mega.privacy.android.data.mapper.OfflineInformationMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.node.NodeShareKeyResultMapper
import mega.privacy.android.data.mapper.shares.AccessPermissionMapper
import mega.privacy.android.data.mapper.shares.ShareDataMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.exception.SynchronisationException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

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
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper,
    private val offlineInformationMapper: OfflineInformationMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    private val streamingGateway: StreamingGateway,
    private val nodeUpdateMapper: NodeUpdateMapper,
    private val accessPermissionMapper: AccessPermissionMapper,
    private val nodeShareKeyResultMapper: NodeShareKeyResultMapper,
) : NodeRepository {


    override suspend fun getOutgoingSharesNode(order: SortOrder) =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(order))
                .map { shareDataMapper(it) }
        }

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

    override suspend fun getVerifiedIncomingShares(order: SortOrder) =
        withContext(ioDispatcher) {
            megaApiGateway.getVerifiedIncomingShares(sortOrderIntMapper(order)).map {
                shareDataMapper(it)
            }
        }

    override suspend fun isNodeInRubbish(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInRubbish(it) }
            ?: run {
                Timber.w("isNodeInRubbish returns false because the node with handle $handle was not found")
                false
            }
    }

    override suspend fun isNodeInInbox(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInInbox(it) }
            ?: run {
                Timber.w("isNodeInInbox returns false because the node with handle $handle was not found")
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
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            convertToUnTypedNode(it)
        }
    }

    override suspend fun getNodePathById(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            megaApiGateway.getNodePath(it) ?: ""
        } ?: ""
    }

    override suspend fun getNodeChildren(folderNode: FolderNode): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(folderNode.id.longValue)?.let { parent ->
                megaApiGateway.getChildrenByNode(parent)
                    .map {
                        convertToUnTypedNode(it)
                    }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }
    }

    override suspend fun getNodeChildren(
        nodeId: NodeId,
        order: SortOrder?,
    ): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            return@withContext megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let { parent ->
                val childList = order?.let { sortOrder ->
                    megaApiGateway.getChildrenByNode(
                        parent,
                        sortOrderIntMapper(sortOrder)
                    )
                } ?: run {
                    megaApiGateway.getChildrenByNode(parent)
                }
                childList.map { convertToUnTypedNode(it) }
            } ?: run {
                emptyList()
            }
        }
    }

    override suspend fun getNumVersions(handle: Long): Int = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let {
            megaApiGateway.getNumVersions(it)
        } ?: 0
    }

    override suspend fun getNodeHistoryVersions(handle: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle.longValue)?.let { megaNode ->
            megaApiGateway.getVersions(megaNode).map { version ->
                convertToUnTypedNode(version)
            }
        } ?: throw SynchronisationException("Non null node found be null when fetched from api")
    }

    override suspend fun getFolderTreeInfo(folderNode: FolderNode): FolderTreeInfo =
        withContext(ioDispatcher) {
            val megaNode = megaApiGateway.getMegaNodeByHandle(folderNode.id.longValue)
            suspendCoroutine { continuation ->
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

    override suspend fun isNodeInRubbishOrDeleted(nodeHandle: Long): Boolean =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeHandle)?.let { megaApiGateway.isInRubbish(it) }
                ?: true
        }

    override suspend fun getOfflineNodeInformation(nodeId: NodeId) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.getOfflineInformation(nodeId.longValue)
                ?.let { offlineNodeInformationMapper(it) }
        }

    override suspend fun saveOfflineNodeInformation(
        offlineNodeInformation: OfflineNodeInformation,
        parentNodeId: NodeId?,
    ) = withContext(ioDispatcher) {
        val parent = parentNodeId?.let { parentId ->
            megaLocalStorageGateway.getOfflineInformation(parentId.longValue)
                ?: throw IllegalArgumentException("Parent offline information must have been previously saved in order to have a consistent hierarchy. ParentId: ${parentId.longValue}")
        }
        megaLocalStorageGateway.saveOfflineInformation(
            offlineInformationMapper(offlineNodeInformation, parent?.id)
        )
    }

    override suspend fun convertBase64ToHandle(base64: String): Long = withContext(ioDispatcher) {
        megaApiGateway.base64ToHandle(base64)
    }

    override suspend fun getOfflineNodeInformation(nodeHandle: Long) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.getOfflineInformation(nodeHandle)
                ?.let { offlineNodeInformationMapper(it) }
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

    private suspend fun convertToUnTypedNode(node: MegaNode): UnTypedNode =
        nodeMapper(
            node,
        )

    override suspend fun stopSharingNode(nodeId: NodeId): Unit = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            megaApiGateway.stopSharingNode(it)
        }
    }

    override suspend fun createShareKey(node: TypedNode): (suspend (AccessPermission, String) -> Unit)? {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(node.id.longValue)?.let { megaNode ->
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener("openShareDialog") {
                        return@getRequestListener nodeShareKeyResultMapper(megaNode)
                    }

                    megaApiGateway.openShareDialog(megaNode, listener)
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(listener)
                    }
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

    override suspend fun loadOfflineNodes(
        path: String,
        searchQuery: String?,
    ): List<OfflineNodeInformation> = withContext(ioDispatcher) {
        megaLocalStorageGateway.loadOfflineNodes(path, searchQuery).map {
            offlineNodeInformationMapper(it)
        }
    }

    override suspend fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

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


    override suspend fun getFileTypeInfo(nodeId: NodeId): FileTypeInfo? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(nodeHandle = nodeId.longValue)?.let { megaNode ->
                return@withContext fileTypeInfoMapper(megaNode)
            }
        }

    override suspend fun getDefaultNodeHandle(folderName: String) = withContext(ioDispatcher) {
        megaApiGateway.getNodeByPath(folderName, megaApiGateway.getRootNode())
            ?.takeIf { it.isFolder && megaApiGateway.isInRubbish(it) }?.let { NodeId(it.handle) }
    }

    override suspend fun checkNodeCanBeMovedToTargetNode(
        nodeId: NodeId,
        targetNodeId: NodeId,
    ): Boolean {
        val node = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
        val targetNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)

        return withContext(ioDispatcher) {
            if (node != null && targetNode != null) {
                megaExceptionMapper(
                    megaApiGateway.checkMoveErrorExtended(
                        node,
                        targetNode
                    )
                ).errorCode != MegaError.API_OK
            } else {
                false
            }
        }
    }

    override suspend fun copyNode(
        nodeToCopy: NodeId,
        newNodeParent: NodeId,
        newNodeName: String?,
    ): NodeId = withContext(ioDispatcher) {
        val node = getMegaNodeByHandle(nodeToCopy, true)
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
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
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
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
        return@withContext when {
            result.second.errorCode == MegaError.API_OK -> NodeId(result.first.nodeHandle)
            result.second.errorCode == MegaError.API_EOVERQUOTA
                    && megaApiGateway.isForeignNode(newNodeParent.longValue) -> throw ForeignNodeException()

            else -> throw result.second.toException("moveNode")
        }
    }

    override suspend fun getFingerprint(filePath: String) = withContext(ioDispatcher) {
        megaApiGateway.getFingerprint(filePath)
    }

    override suspend fun getParentNode(nodeId: NodeId) = withContext(ioDispatcher) {
        val megaNode = megaApiGateway.getMegaNodeByHandle(nodeId.longValue)
        megaNode?.let {
            megaApiGateway.getParentNode(megaNode)?.let { nodeMapper(it) }
        }
    }

    override suspend fun getNodeByOriginalFingerprint(
        originalFingerprint: String,
        parentNodeId: NodeId?,
    ): UnTypedNode? = withContext(ioDispatcher) {
        val megaNode =
            parentNodeId?.let { megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue) }
        megaApiGateway.getNodesByOriginalFingerprint(originalFingerprint, megaNode)?.let {
            if (it.size() > 0) {
                return@let nodeMapper(it[0])
            }
            return@let null
        }.also {
            Timber.d("Found node by original fingerprint with the same local fingerprint in node with handle: ${parentNodeId}, node : $it")
        }
    }

    override suspend fun getNodeByFingerprintAndParentNode(
        fingerprint: String,
        parentNodeId: NodeId,
    ): UnTypedNode? = withContext(ioDispatcher) {
        val megaNode = megaApiGateway.getMegaNodeByHandle(parentNodeId.longValue)
        megaApiGateway.getNodeByFingerprintAndParentNode(fingerprint, megaNode)
            ?.let { nodeMapper(it) }.also {
                Timber.d("Found node by fingerprint with the same local fingerprint in node with handle: ${parentNodeId}, node: $it")
            }
    }

    override suspend fun getNodeByFingerprint(fingerprint: String) =
        withContext(ioDispatcher) {
            megaApiGateway.getNodeByFingerprint(fingerprint)?.let { nodeMapper(it) }
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
            megaApiGateway.getChildNode(parent, name)?.let { nodeMapper(it) }
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
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
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
                ?.let { nodeMapper(it) }
        }

    override suspend fun getNodesByHandles(handles: List<Long>): List<UnTypedNode> =
        handles.mapNotNull { handle ->
            megaApiGateway.getMegaNodeByHandle(handle)
        }.map { node ->
            convertToUnTypedNode(node)
        }

    override suspend fun getRubbishNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getRubbishBinNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getInboxNode(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiGateway.getInboxNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getRootNodeFromMegaApiFolder(): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getRootNode()?.let { megaNode ->
                convertToUnTypedNode(megaNode)
            }
        }

    override suspend fun getParentNodeFromMegaApiFolder(parentHandle: Long): UnTypedNode? =
        withContext(ioDispatcher) {
            megaApiFolderGateway.getMegaNodeByHandle(parentHandle)?.let { megaNode ->
                convertToUnTypedNode(megaNode)
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
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
    }

    override suspend fun exportNode(nodeToExport: NodeId, expireTime: Long?): String =
        withContext(ioDispatcher) {
            val node = getMegaNodeByHandle(nodeToExport, true)
            requireNotNull(node) { "Node to export with handle ${nodeToExport.longValue} not found" }
            require(!node.isTakenDown) { "Node to export with handle ${nodeToExport.longValue} not found" }

            if (node.isExported && !node.isExpired && node.expirationTime == expireTime) {
                return@withContext node.publicLink
            }
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("exportNode") { it.link }
                megaApiGateway.exportNode(node, expireTime, listener)
                continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
            }
        }
}
