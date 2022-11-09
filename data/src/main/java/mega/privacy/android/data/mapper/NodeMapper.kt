package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.node.DefaultFileNode
import mega.privacy.android.data.model.node.DefaultFolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import nz.mega.sdk.MegaNode

/**
 * The mapper class for converting the data entity to FavouriteInfo
 */
typealias NodeMapper = @JvmSuppressWildcards suspend (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards MapThumbnail,
    @JvmSuppressWildcards MapHasVersion,
    @JvmSuppressWildcards MapNumberOfChildFolders,
    @JvmSuppressWildcards MapNumberOfChildFiles,
    @JvmSuppressWildcards FileTypeInfoMapper,
    @JvmSuppressWildcards MapPendingShare,
    @JvmSuppressWildcards MapInRubbish,
) -> @JvmSuppressWildcards UnTypedNode

internal typealias MapThumbnail = suspend (MegaNode) -> String?
internal typealias MapHasVersion = suspend (MegaNode) -> Boolean
internal typealias MapNumberOfChildFolders = suspend (MegaNode) -> Int
internal typealias MapNumberOfChildFiles = suspend (MegaNode) -> Int
internal typealias MapPendingShare = suspend (MegaNode) -> Boolean
internal typealias MapInRubbish = suspend (MegaNode) -> Boolean

internal suspend fun toNode(
    megaNode: MegaNode,
    thumbnailPath: MapThumbnail,
    hasVersion: MapHasVersion,
    numberOfChildFolders: MapNumberOfChildFolders,
    numberOfChildFiles: MapNumberOfChildFiles,
    fileTypeInfoMapper: FileTypeInfoMapper,
    isPendingShare: MapPendingShare,
    isInRubbish: MapInRubbish,
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
        isExported = megaNode.isExported,
        isTakenDown = megaNode.isTakenDown,
        isInRubbishBin = isInRubbish(megaNode),
        isIncomingShare = megaNode.isInShare,
        isShared = megaNode.isOutShare,
        isPendingShare = isPendingShare(megaNode),
        device = megaNode.deviceId,
    )
} else {
    DefaultFileNode(
        id = NodeId(megaNode.handle),
        name = megaNode.name,
        size = megaNode.size,
        label = megaNode.label,
        parentId = NodeId(megaNode.parentHandle),
        base64Id = megaNode.base64Handle,
        modificationTime = megaNode.modificationTime,
        hasVersion = hasVersion(megaNode),
        thumbnailPath = thumbnailPath(megaNode),
        type = fileTypeInfoMapper(megaNode),
        isFavourite = megaNode.isFavourite,
        isExported = megaNode.isExported,
        isTakenDown = megaNode.isTakenDown,
        isIncomingShare = megaNode.isInShare,
        fingerprint = megaNode.fingerprint,
        duration = megaNode.duration
    )
}
