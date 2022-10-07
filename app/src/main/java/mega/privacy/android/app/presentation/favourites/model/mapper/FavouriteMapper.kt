package mega.privacy.android.app.presentation.favourites.model.mapper

import mega.privacy.android.domain.entity.FavouriteFile as FileEntity
import mega.privacy.android.domain.entity.FavouriteFolder as FolderEntity
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FavouriteInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import nz.mega.sdk.MegaNode

/**
 * Mapper for FavouriteInfo convert to Favourite
 */
typealias FavouriteMapper = (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards FavouriteInfo,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards StringUtilWrapper,
    @JvmSuppressWildcards (String) -> Int,
) -> @JvmSuppressWildcards Favourite

/**
 * Convert FavouriteInfo to Favourite
 * @param favouriteInfo FavouriteInfo
 * @param isAvailableOffline isAvailableOffline
 * @param stringUtil StringUtilWrapper
 * @param getFileIcon getFileIcon
 * @return Favourite
 */
internal fun toFavourite(
    node: MegaNode,
    favouriteInfo: FavouriteInfo,
    isAvailableOffline: Boolean,
    stringUtil: StringUtilWrapper,
    getFileIcon: (String) -> Int = { 0 },
) = when (favouriteInfo) {
    is FolderEntity -> {
        favouriteInfo.createFolder(
            node,
            getFolderInfo(favouriteInfo, stringUtil),
            isAvailableOffline,
        )
    }
    is FileEntity -> {
        favouriteInfo.createFile(
            node,
            getFileInfo(favouriteInfo, stringUtil),
            isAvailableOffline,
            getFileIcon,
        )
    }
}


/**
 * Create favourite folder based on favourite info
 * @param node
 * @param folderInfo folder info
 * @param isAvailableOffline whether is available for offline
 * @return FavouriteFolder
 */
private fun FolderEntity.createFolder(
    node: MegaNode,
    folderInfo: String,
    isAvailableOffline: Boolean,
) = FavouriteFolder(
    handle = id,
    icon = MegaNodeUtil.getFolderIcon(node,
        DrawerItem.HOMEPAGE),
    name = name,
    label = label,
    labelColour = MegaNodeUtil.getNodeLabelColor(label),
    showLabel = label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = hasVersion,
    info = folderInfo,
    isFavourite = isFavourite,
    isExported = isExported,
    isTakenDown = isTakenDown,
    isAvailableOffline = isAvailableOffline
)

/**
 * Create favourite file based on favourite info
 * @param node
 * @param fileInfo file info
 * @param isAvailableOffline whether is available for offline
 * @param getFileIcon getFileIcon
 * @return FavouriteFile
 */
private fun FileEntity.createFile(
    node: MegaNode,
    fileInfo: String,
    isAvailableOffline: Boolean,
    getFileIcon: (String) -> Int,
) = FavouriteFile(
    handle = id,
    icon = getFileIcon(name),
    name = name,
    label = label,
    labelColour = MegaNodeUtil.getNodeLabelColor(label),
    showLabel = label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = hasVersion,
    info = fileInfo,
    size = size,
    modificationTime = modificationTime,
    isFavourite = isFavourite,
    isExported = isExported,
    isTakenDown = isTakenDown,
    isAvailableOffline = isAvailableOffline,
    thumbnailPath = thumbnailPath?.takeIf { type.hasThumbnail() }
)

private fun FileTypeInfo.hasThumbnail(): Boolean = when (this) {
    is AudioFileTypeInfo -> true
    is VideoFileTypeInfo -> true
    is ImageFileTypeInfo -> true
    PdfFileTypeInfo -> true
    else -> false
}

/**
 * Needs to happen on the fragment. preferably using Android formatter:
 * android.text.format.Formatter.formatShortFileSize(activityContext, bytes) for size
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return file info
 */
private fun getFileInfo(favouriteInfo: FileEntity, stringUtil: StringUtilWrapper) =
    String.format(
        "%s · %s",
        stringUtil.getSizeString(favouriteInfo.size),
        stringUtil.formatLongDateTime(favouriteInfo.modificationTime)
    )

/**
 * Get folder info based on number of child folders and files
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return folder info
 */
private fun getFolderInfo(favouriteInfo: FolderEntity, stringUtil: StringUtilWrapper) =
    stringUtil.getFolderInfo(favouriteInfo.numChildFolders, favouriteInfo.numChildFiles)