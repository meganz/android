package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import javax.inject.Inject

internal class OfflineImagePreviewMenuOptions @Inject constructor() : ImagePreviewMenuOptions {
    override suspend fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean {
        return imageNode.type !is VideoFileTypeInfo
    }

    override suspend fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isForwardOptionVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isSendToOptionVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isMoreOptionVisible(imageNode: ImageNode): Boolean {
        return true
    }
}
