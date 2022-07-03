package mega.privacy.android.app.data.mapper

import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
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
) -> @JvmSuppressWildcards FavouriteInfo

internal fun toFavouriteInfo(
    megaNode: MegaNode,
    thumbnailPath: String?,
    hasVersion: Boolean,
    numberOfChildFolders: Int,
    numberOfChildFiles: Int,
) = FavouriteInfo(
    id = megaNode.handle,
    name = megaNode.name,
    size = megaNode.size,
    label = megaNode.label,
    parentId = megaNode.parentHandle,
    base64Id = megaNode.base64Handle,
    modificationTime = megaNode.modificationTime,
    hasVersion = hasVersion,
    numChildFolders = numberOfChildFolders,
    numChildFiles = numberOfChildFiles,
    thumbnailPath = thumbnailPath,
    isFolder = megaNode.isFolder,
    isImage = megaNode.isImage(),
    isVideo = megaNode.isVideo(),
    isFavourite = megaNode.isFavourite,
    isExported = megaNode.isExported,
    isTakenDown = megaNode.isTakenDown,
)