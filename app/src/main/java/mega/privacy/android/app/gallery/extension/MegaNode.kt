package mega.privacy.android.app.gallery.extension

import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaNode

val MegaNode.thumbnailPath: String
    get() = this.base64Handle + FileUtil.JPG_EXTENSION