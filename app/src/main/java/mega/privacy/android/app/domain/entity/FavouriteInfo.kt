package mega.privacy.android.app.domain.entity

import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.MegaNode

/**
 * The entity for favourite info
 * @param id current favourite node handle
 * @param parentId current favourite node parent handle
 * @param base64Id current favourite node base64handle
 * @param node current favourite node
 * @param hasVersion whether current favourite item has version
 * @param numChildFolders child folders number
 * @param numChildFiles child files number
 */
data class FavouriteInfo(
    val id: Long,
    val parentId:Long,
    val base64Id: String,
    val modificationTime: Long,
    val node: MegaNode,
    val hasVersion: Boolean,
    val numChildFolders: Int,
    val numChildFiles: Int,
    val isImage: Boolean = node.isImage(),
    val isVideo: Boolean = node.isVideo()
)