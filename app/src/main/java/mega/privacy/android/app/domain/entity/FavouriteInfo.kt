package mega.privacy.android.app.domain.entity

import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.MegaNode

/**
 * The entity for favourite info
 * @param id current favourite node handle
 * @param name current favourite node name
 * @param size current favourite node size
 * @param label current favourite node label
 * @param parentId current favourite node parent handle
 * @param base64Id current favourite node base64handle
 * @property modificationTime current favourite node modificationTime
 * @param node current favourite node
 * @param hasVersion whether current favourite item has version
 * @param numChildFolders child folders number
 * @param numChildFiles child files number
 * @param isImage node type. True is image, otherwise False
 * @param isVideo node type. True is video, otherwise False
 * @param isFolder node whether is folder. True is folder, otherwise False
 * @param thumbnailPath thumbnail file path
 */
data class FavouriteInfo(
    val id: Long,
    val name: String,
    val parentId: Long,
    val base64Id: String,
    val size: Long,
    val label: Int,
    val modificationTime: Long,
    val node: MegaNode,
    val hasVersion: Boolean,
    val numChildFolders: Int,
    val numChildFiles: Int,
    val isImage: Boolean = node.isImage(),
    val isVideo: Boolean = node.isVideo(),
    val isFolder: Boolean = node.isFolder,
    val thumbnailPath: String? = null,
)