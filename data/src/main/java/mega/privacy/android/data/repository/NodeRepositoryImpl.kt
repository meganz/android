package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
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
import mega.privacy.android.data.mapper.MegaShareMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.mapper.OfflineNodeInformationMapper
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.SynchronisationException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
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
 * @property megaShareMapper
 * @property megaExceptionMapper
 * @property sortOrderIntMapper
 * @property cacheFolderGateway
 * @property nodeMapper
 * @property fileTypeInfoMapper
 * @property offlineNodeInformationMapper
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
    private val megaShareMapper: MegaShareMapper,
    private val megaExceptionMapper: MegaExceptionMapper,
    private val sortOrderIntMapper: SortOrderIntMapper,
    private val cacheFolderGateway: CacheFolderGateway,
    private val nodeMapper: NodeMapper,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val offlineNodeInformationMapper: OfflineNodeInformationMapper,
    private val fileGateway: FileGateway,
    private val chatFilesFolderUserAttributeMapper: ChatFilesFolderUserAttributeMapper,
    private val streamingGateway: StreamingGateway,
) : NodeRepository {


    override suspend fun getOutgoingSharesNode(order: SortOrder): List<ShareData> =
        withContext(ioDispatcher) {
            megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(order))
                .map { megaShareMapper(it) }
        }

    override suspend fun isNodeInRubbish(handle: Long) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let { megaApiGateway.isInRubbish(it) } ?: false
    }


    override suspend fun getBackupFolderId(): NodeId =
        withContext(ioDispatcher) {
            val backupsFolderAttributeIdentifier = MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER
            suspendCancellableCoroutine { continuation ->
                megaApiGateway.getUserAttribute(backupsFolderAttributeIdentifier,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (request.paramType == backupsFolderAttributeIdentifier) {
                                if (error.errorCode == MegaError.API_OK) {
                                    continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                                } else {
                                    continuation.failWithError(error)
                                }
                            }
                        }
                    ))
            }
        }

    override suspend fun getNodeById(nodeId: NodeId) = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(nodeId.longValue)?.let {
            nodeMapper(
                it,
                cacheFolderGateway::getThumbnailCacheFilePath,
                megaApiGateway::hasVersion,
                megaApiGateway::getNumChildFolders,
                megaApiGateway::getNumChildFiles,
                fileTypeInfoMapper,
                megaApiGateway::isPendingShare,
                megaApiGateway::isInRubbish,
            )
        }
    }

    override suspend fun getNodeChildren(folderNode: FolderNode): List<UnTypedNode> {
        return withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(folderNode.id.longValue)?.let { parent ->
                megaApiGateway.getChildrenByNode(parent)
                    .map {
                        nodeMapper(
                            it,
                            cacheFolderGateway::getThumbnailCacheFilePath,
                            megaApiGateway::hasVersion,
                            megaApiGateway::getNumChildFolders,
                            megaApiGateway::getNumChildFiles,
                            fileTypeInfoMapper,
                            megaApiGateway::isPendingShare,
                            megaApiGateway::isInRubbish,
                        )
                    }
            } ?: throw SynchronisationException("Non null node found be null when fetched from api")
        }
    }

    override suspend fun getNodeHistoryNumVersions(handle: Long): Int = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)?.let {
            if (megaApiGateway.hasVersion(it)) {
                megaApiGateway.getNumVersions(it)
            } else {
                0
            }
        } ?: 0
    }

    override fun monitorNodeUpdates(): Flow<List<Node>> {
        return megaApiGateway.globalUpdates
            .filterIsInstance<GlobalUpdate.OnNodesUpdate>()
            .mapNotNull {
                it.nodeList?.map { megaNode ->
                    nodeMapper(
                        megaNode,
                        cacheFolderGateway::getThumbnailCacheFilePath,
                        megaApiGateway::hasVersion,
                        megaApiGateway::getNumChildFolders,
                        megaApiGateway::getNumChildFiles,
                        fileTypeInfoMapper,
                        megaApiGateway::isPendingShare,
                        megaApiGateway::isInRubbish,
                    )
                }
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
}
