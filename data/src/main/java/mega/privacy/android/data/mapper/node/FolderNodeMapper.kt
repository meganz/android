package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.node.DefaultFolderNode
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Folder node mapper
 *
 * @property megaApiGateway
 * @constructor Create empty Folder node mapper
 */
internal class FolderNodeMapper @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val fetChildrenMapper: FetchChildrenMapper,
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
        parentId = NodeId(megaNode.parentHandle),
        base64Id = megaNode.base64Handle,
        childFolderCount = if (fromFolderLink)
            megaApiFolderGateway.getNumChildFolders(megaNode)
        else
            megaApiGateway.getNumChildFolders(megaNode),
        childFileCount = if (fromFolderLink)
            megaApiFolderGateway.getNumChildFiles(megaNode)
        else
            megaApiGateway.getNumChildFiles(megaNode),
        isFavourite = megaNode.isFavourite,
        exportedData = megaNode.takeIf { megaNode.isExported }?.let {
            ExportedData(it.publicLink, it.publicLinkCreationTime)
        },
        isTakenDown = megaNode.isTakenDown,
        isInRubbishBin = megaApiGateway.isInRubbish(megaNode),
        isIncomingShare = megaNode.isInShare,
        isShared = megaNode.isOutShare,
        isPendingShare = megaApiGateway.isPendingShare(megaNode),
        device = megaNode.deviceId,
        isNodeKeyDecrypted = megaNode.isNodeKeyDecrypted,
        creationTime = megaNode.creationTime,
        fetchChildren = fetChildrenMapper(megaNode),
        serializedData = if (requireSerializedData) megaNode.serialize() else null,
        isAvailableOffline = isAvailableOffline,
        versionCount = (megaApiGateway.getNumVersions(megaNode) - 1).coerceAtLeast(0)
    )
}