package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import javax.inject.Inject

class TimelineImagePreviewMenuOptions @Inject constructor() : ImagePreviewMenuOptions {

    override suspend fun isSlideshowOptionVisible(imageNode: ImageNode) =
        imageNode.type !is VideoFileTypeInfo

    override suspend fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isForwardOptionVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isSendToOptionVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isMoreOptionVisible(imageNode: ImageNode): Boolean = true

}