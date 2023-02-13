package mega.privacy.android.data.extensions

import mega.privacy.android.data.constant.FileConstant.JPG_EXTENSION
import mega.privacy.android.data.model.MimeTypeList
import nz.mega.sdk.MegaNode

/**
 * Get Thumbnail Image File Name
 */
fun MegaNode.getThumbnailFileName(): String =
    "$base64Handle${JPG_EXTENSION}"

/**
 * Get Preview Image File Name
 */
fun MegaNode.getPreviewFileName(): String =
    "$base64Handle${JPG_EXTENSION}"

/**
 * Get Full Image File Name
 */
fun MegaNode.getFileName(): String =
    "$base64Handle.${MimeTypeList.typeForName(name).extension}"

/**
 * Check if Node is a Video
 */
fun MegaNode.isVideo() =
    this.isFile && (MimeTypeList.typeForName(name).isVideoMimeType() ||
            MimeTypeList.typeForName(name).isMp4Video())