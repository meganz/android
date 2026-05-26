package mega.privacy.android.data.extensions

import mega.privacy.android.data.constant.FileConstant.JPG_EXTENSION
import nz.mega.sdk.MegaNode

/**
 * Get Thumbnail Image File Name
 */
fun MegaNode?.getThumbnailFileName(): String = this?.base64Handle.orEmpty()

/**
 * Get Preview Image File Name
 */
fun MegaNode?.getPreviewFileName(): String =
    this?.base64Handle?.let { "$it$JPG_EXTENSION" } ?: ""

/**
 * Get Full Image File Name
 */
fun MegaNode?.getFileName(): String =
    "${this?.base64Handle}.${this?.name?.substringAfterLast(".", "")}"
