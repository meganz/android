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
) =
    if (favouriteInfo.isFolder) {
        createFolder(
            node,
            favouriteInfo,
            getFolderInfo(favouriteInfo, stringUtil),
            isAvailableOffline,
        )
    } else {
        createFile(
            node,
            favouriteInfo,
            getFileInfo(favouriteInfo, stringUtil),
            isAvailableOffline,
            getFileIcon,
        )
    }

/**
 * Create favourite folder based on favourite info
 * @param favouriteInfo FavouriteInfo
 * @param folderInfo folder info
 * @param isAvailableOffline whether is available for offline
 * @return FavouriteFolder
 */
private fun createFolder(
    node: MegaNode,
    favouriteInfo: FavouriteInfo,
    folderInfo: String,
    isAvailableOffline: Boolean,
) = FavouriteFolder(
    handle = favouriteInfo.id,
    icon = MegaNodeUtil.getFolderIcon(node,
        DrawerItem.HOMEPAGE),
    name = favouriteInfo.name,
    label = favouriteInfo.label,
    size = favouriteInfo.size,
    modificationTime = favouriteInfo.modificationTime,
    labelColour = MegaNodeUtil.getNodeLabelColor(favouriteInfo.label),
    showLabel = favouriteInfo.label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = favouriteInfo.hasVersion,
    info = folderInfo,
    isFavourite = favouriteInfo.isFavourite,
    isExported = favouriteInfo.isExported,
    isTakenDown = favouriteInfo.isTakenDown,
    isAvailableOffline = isAvailableOffline
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
    node: MegaNode,
    favouriteInfo: FavouriteInfo,
    fileInfo: String,
    isAvailableOffline: Boolean,
    getFileIcon: (String) -> Int,
) = FavouriteFile(
    handle = favouriteInfo.id,
    icon = getFileIcon(favouriteInfo.name),
    name = favouriteInfo.name,
    label = favouriteInfo.label,
    labelColour = MegaNodeUtil.getNodeLabelColor(favouriteInfo.label),
    showLabel = favouriteInfo.label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = favouriteInfo.hasVersion,
    info = fileInfo,
    size = favouriteInfo.size,
    modificationTime = favouriteInfo.modificationTime,
    isFavourite = favouriteInfo.isFavourite,
    isExported = favouriteInfo.isExported,
    isTakenDown = favouriteInfo.isTakenDown,
    isAvailableOffline = isAvailableOffline,
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
        stringUtil.getSizeString(favouriteInfo.size),
        stringUtil.formatLongDateTime(favouriteInfo.modificationTime)
    )

/**
 * Get folder info based on number of child folders and files
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return folder info
 */
private fun getFolderInfo(favouriteInfo: FavouriteInfo, stringUtil: StringUtilWrapper) =
    stringUtil.getFolderInfo(favouriteInfo.numChildFolders, favouriteInfo.numChildFiles)