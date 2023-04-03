package mega.privacy.android.app.presentation.favourites.model.mapper

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.node.model.mapper.getDefaultFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import nz.mega.sdk.MegaNode

/**
 * Mapper for FavouriteInfo convert to Favourite
 */
typealias FavouriteMapper = (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards TypedNode,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards StringUtilWrapper,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards (String) -> Int,
) -> @JvmSuppressWildcards Favourite


/**
 * Convert NodeInfo to Favourite
 * @param nodeInfo FavouriteInfo
 * @param isAvailableOffline isAvailableOffline
 * @param stringUtil StringUtilWrapper
 * @param getFileIcon getFileIcon
 * @return Favourite
 */
internal fun toFavourite(
    node: MegaNode,
    nodeInfo: TypedNode,
    isAvailableOffline: Boolean,
    stringUtil: StringUtilWrapper,
    isSelected: Boolean,
    getFileIcon: (String) -> Int = { 0 },
) = when (nodeInfo) {
    is TypedFolderNode -> {
        nodeInfo.createFolder(
            node,
            getFolderInfo(nodeInfo),
            isAvailableOffline,
            isSelected,
        )
    }
    is TypedFileNode -> {
        nodeInfo.createFile(
            node,
            getFileInfo(nodeInfo, stringUtil),
            isAvailableOffline,
            getFileIcon,
            isSelected,
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
private fun TypedFolderNode.createFolder(
    node: MegaNode,
    folderInfo: (Context) -> String,
    isAvailableOffline: Boolean,
    isSelected: Boolean,
) = FavouriteFolder(
    icon = getDefaultFolderIcon(this),
    labelColour = MegaNodeUtil.getNodeLabelColor(label),
    showLabel = label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    info = folderInfo,
    isAvailableOffline = isAvailableOffline,
    typedNode = this,
    isSelected = isSelected,
)

/**
 * Create favourite file based on favourite info
 * @param node
 * @param fileInfo file info
 * @param isAvailableOffline whether is available for offline
 * @param getFileIcon getFileIcon
 * @return FavouriteFile
 */
private fun TypedFileNode.createFile(
    node: MegaNode,
    fileInfo: (Context) -> String,
    isAvailableOffline: Boolean,
    getFileIcon: (String) -> Int,
    isSelected: Boolean,
) = FavouriteFile(
    icon = getFileIcon(name),
    labelColour = MegaNodeUtil.getNodeLabelColor(label),
    showLabel = label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    info = fileInfo,
    isAvailableOffline = isAvailableOffline,
    thumbnailPath = thumbnailPath?.takeIf { type.hasThumbnail() },
    typedNode = this,
    isSelected = isSelected,
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
private fun getFileInfo(favouriteInfo: TypedFileNode, stringUtil: StringUtilWrapper) =
    { context: Context ->
        String.format(
            "%s · %s",
            stringUtil.getSizeString(favouriteInfo.size, context),
            stringUtil.formatLongDateTime(favouriteInfo.modificationTime)
        )
    }

/**
 * Get folder info based on number of child folders and files
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return folder info
 */
private fun getFolderInfo(
    favouriteInfo: TypedFolderNode,
) = { context: Context ->
    if (favouriteInfo.childFolderCount == 0 && favouriteInfo.childFileCount == 0) {
        context.getString(R.string.file_browser_empty_folder)
    } else if (favouriteInfo.childFolderCount == 0 && favouriteInfo.childFileCount > 0) {
        context.resources.getQuantityString(
            R.plurals.num_files_with_parameter,
            favouriteInfo.childFileCount,
            favouriteInfo.childFileCount
        )
    } else if (favouriteInfo.childFileCount == 0 && favouriteInfo.childFolderCount > 0) {
        context.resources.getQuantityString(
            R.plurals.num_folders_with_parameter,
            favouriteInfo.childFolderCount,
            favouriteInfo.childFolderCount
        )
    } else {
        context.resources.getQuantityString(
            R.plurals.num_folders_num_files,
            favouriteInfo.childFolderCount,
            favouriteInfo.childFolderCount
        ) + context.resources.getQuantityString(
            R.plurals.num_folders_num_files_2,
            favouriteInfo.childFileCount,
            favouriteInfo.childFileCount
        )
    }
}