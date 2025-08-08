package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.data.model.node.DefaultFolderNode
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Folder node mapper
 *
 * @property megaApiGateway
 * @property megaApiFolderGateway
 * @property fetChildrenMapper
 * @property stringListMapper
 * @constructor Create empty Folder node mapper
 */
internal class FolderNodeMapper @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val fetChildrenMapper: FetchChildrenMapper,
    private val stringListMapper: StringListMapper,
    private val nodeLabelMapper: NodeLabelMapper
) {
    /**
     * Invoke
     *
     * @param megaNode
     * @param requireSerializedData
     * @return
     */
    suspend operator fun invoke(
        megaNode: MegaNode,
        fromFolderLink: Boolean,
        requireSerializedData: Boolean,
        isAvailableOffline: Boolean
    ): FolderNode = DefaultFolderNode(
        id = NodeId(megaNode.handle),
        name = megaNode.name,
        label = megaNode.label,
        nodeLabel = nodeLabelMapper(megaNode.label),
        parentId = NodeId(megaNode.parentHandle),
        base64Id = megaNode.base64Handle,
        restoreId = NodeId(megaNode.restoreHandle).takeIf {
            it.longValue != MegaApiJava.INVALID_HANDLE
        },
        childFolderCount = if (fromFolderLink)
            megaApiFolderGateway.getNumChildFolders(megaNode)
        else
            megaApiGateway.getNumChildFolders(megaNode),
        childFileCount = if (fromFolderLink)
            megaApiFolderGateway.getNumChildFiles(megaNode)
        else
            megaApiGateway.getNumChildFiles(megaNode),
        isFavourite = megaNode.isFavourite,
        isMarkedSensitive = megaNode.isMarkedSensitive,
        isSensitiveInherited = megaApiGateway.isSensitiveInherited(megaNode),
        exportedData = megaNode.takeIf { megaNode.isExported }?.let {
            ExportedData(it.publicLink, it.publicLinkCreationTime)
        },
        isTakenDown = megaNode.isTakenDown,
        isInRubbishBin = megaApiGateway.isInRubbish(megaNode),
        isIncomingShare = megaNode.isInShare,
        isShared = megaNode.isOutShare,
        isPendingShare = megaApiGateway.isPendingShare(megaNode),
        isSynced = isSynced(megaNode),
        device = megaNode.deviceId,
        isNodeKeyDecrypted = megaNode.isNodeKeyDecrypted,
        creationTime = megaNode.creationTime,
        fetchChildren = fetChildrenMapper(megaNode),
        serializedData = if (requireSerializedData) megaNode.serialize() else null,
        isAvailableOffline = isAvailableOffline,
        versionCount = (megaApiGateway.getNumVersions(megaNode) - 1).coerceAtLeast(0),
        description = megaNode.description,
        tags = megaNode.tags?.let { stringListMapper(it) }
    )

    private fun isSynced(megaNode: MegaNode): Boolean {
        val syncs = megaApiGateway.getSyncs()
        for (i in 0..syncs.size()) {
            syncs.get(i)?.let { syncNode ->
                if (syncNode.megaHandle == megaNode.handle) {
                    return true
                }
            }
        }
        return false
    }
}