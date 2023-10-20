package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.node.ImageNode

interface ImagePreviewMenuOptions {

    fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean

    fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean

    fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean

    fun isForwardOptionVisible(imageNode: ImageNode): Boolean

    fun isSendToOptionVisible(imageNode: ImageNode): Boolean
}