package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.data.model.node.DefaultFolderNode
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The mapper class for converting the data entity to FavouriteInfo
 */
internal class NodeMapper @Inject constructor() {
    suspend operator fun invoke(
        megaNode: MegaNode,
        thumbnailPath: suspend (MegaNode) -> String?,
        previewPath: suspend (MegaNode) -> String?,
        fullSizePath: suspend (MegaNode) -> String?,
        hasVersion: suspend (MegaNode) -> Boolean,
        numberOfChildFolders: suspend (MegaNode) -> Int,
        numberOfChildFiles: suspend (MegaNode) -> Int,
        fileTypeInfoMapper: FileTypeInfoMapper,
        isPendingShare: suspend (MegaNode) -> Boolean,
        isInRubbish: suspend (MegaNode) -> Boolean,
    ) = if (megaNode.isFolder) {
        DefaultFolderNode(
            id = NodeId(megaNode.handle),
            name = megaNode.name,
            label = megaNode.label,
            parentId = NodeId(megaNode.parentHandle),
            base64Id = megaNode.base64Handle,
            hasVersion = hasVersion(megaNode),
            childFolderCount = numberOfChildFolders(megaNode),
            childFileCount = numberOfChildFiles(megaNode),
            isFavourite = megaNode.isFavourite,
            exportedData = megaNode.takeIf { megaNode.isExported }?.let {
                ExportedData(it.publicLink, it.publicLinkCreationTime)
            },
            isTakenDown = megaNode.isTakenDown,
            isInRubbishBin = isInRubbish(megaNode),
            isIncomingShare = megaNode.isInShare,
            isShared = megaNode.isOutShare,
            isPendingShare = isPendingShare(megaNode),
            device = megaNode.deviceId,
            isNodeKeyDecrypted = megaNode.isNodeKeyDecrypted,
            creationTime = megaNode.creationTime,
        )
    } else {
        DefaultFileNode(
            id = NodeId(megaNode.handle),
            name = megaNode.name,
            size = megaNode.size,
            label = megaNode.label,
            parentId = NodeId(megaNode.parentHandle),
            base64Id = megaNode.base64Handle,
            creationTime = megaNode.creationTime,
            modificationTime = megaNode.modificationTime,
            hasVersion = hasVersion(megaNode),
            thumbnailPath = thumbnailPath(megaNode),
            previewPath = previewPath(megaNode),
            fullSizePath = fullSizePath(megaNode),
            type = fileTypeInfoMapper(megaNode),
            isFavourite = megaNode.isFavourite,
            exportedData = megaNode.takeIf { megaNode.isExported }?.let {
                ExportedData(it.publicLink, it.publicLinkCreationTime)
            },
            isTakenDown = megaNode.isTakenDown,
            isIncomingShare = megaNode.isInShare,
            fingerprint = megaNode.fingerprint,
            isNodeKeyDecrypted = megaNode.isNodeKeyDecrypted,
            hasThumbnail = megaNode.hasThumbnail(),
            hasPreview = megaNode.hasPreview(),
        )
    }
}
