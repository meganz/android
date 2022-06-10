package mega.privacy.android.app.presentation.mapper

import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.utils.MegaNodeUtil
import nz.mega.sdk.MegaNode

/**
 * Mapper for FavouriteInfo convert to Favourite
 */
typealias FavouriteMapper = (
    @JvmSuppressWildcards FavouriteInfo,
    @JvmSuppressWildcards (MegaNode) -> Boolean,
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
    favouriteInfo: FavouriteInfo, isAvailableOffline: (MegaNode) -> Boolean,
    stringUtil: StringUtilWrapper, getFileIcon: (String) -> Int = { 0 },
) =
    if (favouriteInfo.node.isFolder) {
        createFolder(favouriteInfo, getFolderInfo(favouriteInfo, stringUtil), isAvailableOffline)
    } else {
        createFile(favouriteInfo,
            getFileInfo(favouriteInfo, stringUtil),
            isAvailableOffline,
            getFileIcon)
    }

/**
 * Create favourite folder based on favourite info
 * @param favouriteInfo FavouriteInfo
 * @param folderInfo folder info
 * @param isAvailableOffline whether is available for offline
 * @return FavouriteFolder
 */
private fun createFolder(
    favouriteInfo: FavouriteInfo,
    folderInfo: String,
    isAvailableOffline: (MegaNode) -> Boolean,
) = FavouriteFolder(
    handle = favouriteInfo.id,
    icon = MegaNodeUtil.getFolderIcon(favouriteInfo.node, DrawerItem.HOMEPAGE),
    name = favouriteInfo.name,
    label = favouriteInfo.label,
    size = favouriteInfo.size,
    modificationTime = favouriteInfo.modificationTime,
    labelColour = MegaNodeUtil.getNodeLabelColor(favouriteInfo.label),
    showLabel = favouriteInfo.label != MegaNode.NODE_LBL_UNKNOWN,
    node = favouriteInfo.node,
    hasVersion = favouriteInfo.hasVersion,
    info = folderInfo,
    isFavourite = favouriteInfo.node.isFavourite,
    isExported = favouriteInfo.node.isExported,
    isTakenDown = favouriteInfo.node.isTakenDown,
    isAvailableOffline = isAvailableOffline(favouriteInfo.node)
)

/**
 * Create favourite file based on favourite info
 * @param favouriteInfo FavouriteInfo
 * @param fileInfo file info
 * @param isAvailableOffline whether is available for offline
 * @param getFileIcon getFileIcon
 * @return FavouriteFile
 */
private fun createFile(
    favouriteInfo: FavouriteInfo,
    fileInfo: String,
    isAvailableOffline: (MegaNode) -> Boolean,
    getFileIcon: (String) -> Int,
) = FavouriteFile(
    handle = favouriteInfo.id,
    icon = getFileIcon(favouriteInfo.name),
    name = favouriteInfo.name,
    label = favouriteInfo.label,
    labelColour = MegaNodeUtil.getNodeLabelColor(favouriteInfo.node.label),
    showLabel = favouriteInfo.label != MegaNode.NODE_LBL_UNKNOWN,
    node = favouriteInfo.node,
    hasVersion = favouriteInfo.hasVersion,
    info = fileInfo,
    size = favouriteInfo.size,
    modificationTime = favouriteInfo.modificationTime,
    isFavourite = favouriteInfo.node.isFavourite,
    isExported = favouriteInfo.node.isExported,
    isTakenDown = favouriteInfo.node.isTakenDown,
    isAvailableOffline = isAvailableOffline(favouriteInfo.node),
    thumbnailPath = favouriteInfo.thumbnailPath
)

/**
 * Needs to happen on the fragment. preferably using Android formatter:
 * android.text.format.Formatter.formatShortFileSize(activityContext, bytes) for size
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return file info
 */
private fun getFileInfo(favouriteInfo: FavouriteInfo, stringUtil: StringUtilWrapper) =
    String.format(
        "%s · %s",
        stringUtil.getSizeString(favouriteInfo.node.size),
        stringUtil.formatLongDateTime(favouriteInfo.node.modificationTime)
    )

/**
 * Get folder info based on number of child folders and files
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return folder info
 */
private fun getFolderInfo(favouriteInfo: FavouriteInfo, stringUtil: StringUtilWrapper) =
    stringUtil.getFolderInfo(favouriteInfo.numChildFolders, favouriteInfo.numChildFiles)