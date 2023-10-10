package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.node.ImageNode

interface ImagePreviewMenuOptions {

    fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean

    fun isLinkOptionVisible(imageNode: ImageNode): Boolean

    fun isDownloadOptionVisible(imageNode: ImageNode): Boolean

    fun isForwardOptionVisible(imageNode: ImageNode): Boolean
}