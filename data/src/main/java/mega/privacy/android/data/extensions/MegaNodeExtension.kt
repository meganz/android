package mega.privacy.android.data.extensions

import mega.privacy.android.data.constant.FileConstant.JPG_EXTENSION
import mega.privacy.android.data.model.MimeTypeList
import nz.mega.sdk.MegaNode

fun MegaNode.getThumbnailFileName(): String =
    "$base64Handle${JPG_EXTENSION}"

fun MegaNode.getPreviewFileName(): String =
    "$base64Handle${JPG_EXTENSION}"

fun MegaNode.getFileName(): String =
    "$base64Handle.${MimeTypeList.typeForName(name).extension}"