package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.node.ImageNode

interface ImagePreviewMenuOptions {
    suspend fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean

    suspend fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean

    suspend fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean

    suspend fun isForwardOptionVisible(imageNode: ImageNode): Boolean

    suspend fun isSendToOptionVisible(imageNode: ImageNode): Boolean

    suspend fun isMoreOptionVisible(imageNode: ImageNode): Boolean
}