package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.NodeFile
import mega.privacy.android.domain.entity.NodeFolder
import mega.privacy.android.domain.entity.NodeInfo
import nz.mega.sdk.MegaNode

/**
 * The mapper class for converting the data entity to FavouriteInfo
 */
typealias NodeInfoMapper = @JvmSuppressWildcards suspend (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards MapThumbnail,
    @JvmSuppressWildcards MapHasVersion,
    @JvmSuppressWildcards MapNumberOfChildFolders,
    @JvmSuppressWildcards MapNumberOfChildFiles,
    @JvmSuppressWildcards FileTypeInfoMapper,
    @JvmSuppressWildcards MapPendingShare,
    @JvmSuppressWildcards MapInRubbish,
) -> @JvmSuppressWildcards NodeInfo

internal typealias MapThumbnail = suspend (MegaNode) -> String?
internal typealias MapHasVersion = suspend (MegaNode) -> Boolean
internal typealias MapNumberOfChildFolders = suspend (MegaNode) -> Int
internal typealias MapNumberOfChildFiles = suspend (MegaNode) -> Int
internal typealias MapPendingShare = suspend (MegaNode) -> Boolean
internal typealias MapInRubbish = suspend (MegaNode) -> Boolean

internal suspend fun toNodeInfo(
    megaNode: MegaNode,
    thumbnailPath: MapThumbnail,
    hasVersion: MapHasVersion,
    numberOfChildFolders: MapNumberOfChildFolders,
    numberOfChildFiles: MapNumberOfChildFiles,
    fileTypeInfoMapper: FileTypeInfoMapper,
    isPendingShare: MapPendingShare,
    isInRubbish: MapInRubbish,
) = if (megaNode.isFolder) {
    NodeFolder(
        id = megaNode.handle,
        name = megaNode.name,
        label = megaNode.label,
        parentId = megaNode.parentHandle,
        base64Id = megaNode.base64Handle,
        hasVersion = hasVersion(megaNode),
        numChildFolders = numberOfChildFolders(megaNode),
        numChildFiles = numberOfChildFiles(megaNode),
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
    NodeFile(
        id = megaNode.handle,
        name = megaNode.name,
        size = megaNode.size,
        label = megaNode.label,
        parentId = megaNode.parentHandle,
        base64Id = megaNode.base64Handle,
        modificationTime = megaNode.modificationTime,
        hasVersion = hasVersion(megaNode),
        thumbnailPath = thumbnailPath(megaNode),
        type = fileTypeInfoMapper(megaNode),
        isFavourite = megaNode.isFavourite,
        isExported = megaNode.isExported,
        isTakenDown = megaNode.isTakenDown,
    )
}


