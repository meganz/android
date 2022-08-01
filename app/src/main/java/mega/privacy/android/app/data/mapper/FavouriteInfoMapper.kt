package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.FavouriteFile
import mega.privacy.android.domain.entity.FavouriteFolder
import mega.privacy.android.domain.entity.FavouriteInfo
import nz.mega.sdk.MegaNode

/**
 * The mapper class for converting the data entity to FavouriteInfo
 */
typealias FavouriteInfoMapper = (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards Int,
    @JvmSuppressWildcards FileTypeInfoMapper,
) -> @JvmSuppressWildcards FavouriteInfo

internal fun toFavouriteInfo(
    megaNode: MegaNode,
    thumbnailPath: String?,
    hasVersion: Boolean,
    numberOfChildFolders: Int,
    numberOfChildFiles: Int,
    fileTypeInfoMapper: FileTypeInfoMapper,
) = if (megaNode.isFolder) {
    FavouriteFolder(
        id = megaNode.handle,
        name = megaNode.name,
        label = megaNode.label,
        parentId = megaNode.parentHandle,
        base64Id = megaNode.base64Handle,
        hasVersion = hasVersion,
        numChildFolders = numberOfChildFolders,
        numChildFiles = numberOfChildFiles,
        thumbnailPath = thumbnailPath,
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
        hasVersion = hasVersion,
        thumbnailPath = thumbnailPath,
        type = fileTypeInfoMapper(megaNode),
        isFavourite = megaNode.isFavourite,
        isExported = megaNode.isExported,
        isTakenDown = megaNode.isTakenDown,
    )
}


