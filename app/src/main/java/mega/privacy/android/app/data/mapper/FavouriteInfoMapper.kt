package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.FavouriteFile
import mega.privacy.android.domain.entity.FavouriteFolder
import mega.privacy.android.domain.entity.FavouriteInfo
import nz.mega.sdk.MegaNode

/**
 * The mapper class for converting the data entity to FavouriteInfo
 */
typealias FavouriteInfoMapper = @JvmSuppressWildcards suspend (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards MapThumbnail,
    @JvmSuppressWildcards MapHasVersion,
    @JvmSuppressWildcards MapNumberOfChildFolders,
    @JvmSuppressWildcards MapNumberOfChildFiles,
    @JvmSuppressWildcards FileTypeInfoMapper,
) -> @JvmSuppressWildcards FavouriteInfo

internal typealias MapThumbnail = suspend (MegaNode) -> String?
internal typealias MapHasVersion = suspend (MegaNode) -> Boolean
internal typealias MapNumberOfChildFolders = suspend (MegaNode) -> Int
internal typealias MapNumberOfChildFiles = suspend (MegaNode) -> Int
internal typealias MapPendingShare = suspend (MegaNode) -> Boolean
internal typealias MapInRubbish = suspend (MegaNode) -> Boolean

internal suspend fun toFavouriteInfo(
    megaNode: MegaNode,
    thumbnailPath: MapThumbnail,
    hasVersion: MapHasVersion,
    numberOfChildFolders: MapNumberOfChildFolders,
    numberOfChildFiles: MapNumberOfChildFiles,
    fileTypeInfoMapper: FileTypeInfoMapper,
) = if (megaNode.isFolder) {
    FavouriteFolder(
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
    )
} else {
    FavouriteFile(
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


