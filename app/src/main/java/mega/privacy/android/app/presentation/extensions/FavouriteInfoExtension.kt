package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.photos.model.AlbumCoverItem
import mega.privacy.android.app.utils.MegaNodeUtil
import nz.mega.sdk.MegaNode
import java.io.File

/**
 * Convert FavouriteInfo to Favourite
 * @param isAvailableOffline whether is available for offline
 * @param stringUtil StringUtilWrapper
 * @return Favourite
 */
fun FavouriteInfo.toFavourite(
    isAvailableOffline: (MegaNode) -> Boolean,
    stringUtil: StringUtilWrapper
): Favourite =
    if (node.isFolder) {
        createFolder(getFolderInfo(stringUtil), isAvailableOffline)
    } else {
        createFile(getFileInfo(stringUtil), isAvailableOffline)
    }

/**
 * Create favourite folder based on favourite info
 * @param folderInfo folder info
 * @param isAvailableOffline whether is available for offline
 * @return FavouriteFolder
 */
private fun FavouriteInfo.createFolder(
    folderInfo: String,
    isAvailableOffline: (MegaNode) -> Boolean
) = FavouriteFolder(
    handle = node.handle,
    icon = MegaNodeUtil.getFolderIcon(node, DrawerItem.HOMEPAGE),
    name = node.name,
    labelColour = MegaNodeUtil.getNodeLabelColor(node.label),
    showLabel = node.label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = hasVersion,
    info = folderInfo,
    isFavourite = node.isFavourite,
    isExported = node.isExported,
    isTakenDown = node.isTakenDown,
    isAvailableOffline = isAvailableOffline(node)
)

/**
 * Create favourite file based on favourite info
 * @param fileInfo file info
 * @param isAvailableOffline whether is available for offline
 * @return FavouriteFile
 */
private fun FavouriteInfo.createFile(
    fileInfo: String,
    isAvailableOffline: (MegaNode) -> Boolean
) = FavouriteFile(
    handle = node.handle,
    icon = MimeTypeList.typeForName(node.name).iconResourceId,
    name = node.name,
    labelColour = MegaNodeUtil.getNodeLabelColor(node.label),
    showLabel = node.label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = hasVersion,
    info = fileInfo,
    size = node.size,
    lastModifiedTime = node.modificationTime,
    isFavourite = node.isFavourite,
    isExported = node.isExported,
    isTakenDown = node.isTakenDown,
    isAvailableOffline = isAvailableOffline(node),
)

/**
 * Needs to happen on the fragment. preferably using Android formatter:
 * android.text.format.Formatter.formatShortFileSize(activityContext, bytes) for size
 * @param stringUtil StringUtilWrapper
 * @return file info
 */
private fun FavouriteInfo.getFileInfo(stringUtil: StringUtilWrapper) =
    String.format(
        "%s · %s",
        stringUtil.getSizeString(node.size),
        stringUtil.formatLongDateTime(node.modificationTime)
    )

/**
 * Get folder info based on number of child folders and files
 * @param stringUtil StringUtilWrapper
 * @return folder info
 */
private fun FavouriteInfo.getFolderInfo(stringUtil: StringUtilWrapper) =
    stringUtil.getFolderInfo(numChildFolders, numChildFiles)

/**
 * FavouriteInfo to FavoriteAlbumCoverItem
 *
 * @param itemCount
 * @return AlbumCoverItem.
 */
fun FavouriteInfo.toFavoriteAlbumCoverItem(coverThumbnail: File?,itemCount: Int): AlbumCoverItem =
    AlbumCoverItem(
        titleResId = R.string.title_favourites_album,
        coverThumbnail = coverThumbnail,
        itemCount = itemCount.toString()
    )