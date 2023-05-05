package mega.privacy.android.data.mapper.node

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
    private val fetChildrenMapper: FetchChildrenMapper,
) {
    /**
     * Invoke
     *
     * @param megaNode
     * @return
     */
    suspend operator fun invoke(megaNode: MegaNode): FolderNode = DefaultFolderNode(
        id = NodeId(megaNode.handle),
        name = megaNode.name,
        label = megaNode.label,
        parentId = NodeId(megaNode.parentHandle),
        base64Id = megaNode.base64Handle,
        hasVersion = megaApiGateway.hasVersion(megaNode),
        childFolderCount = megaApiGateway.getNumChildFolders(megaNode),
        childFileCount = megaApiGateway.getNumChildFiles(megaNode),
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
    )
}