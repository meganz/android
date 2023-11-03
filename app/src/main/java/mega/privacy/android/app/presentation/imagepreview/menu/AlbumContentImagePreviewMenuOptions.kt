package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import javax.inject.Inject

class AlbumContentImagePreviewMenuOptions @Inject constructor() : ImagePreviewMenuOptions {

    override fun isSlideshowOptionVisible(imageNode: ImageNode) =
        imageNode.type !is VideoFileTypeInfo

    override fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean = true

    override fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean = true

    override fun isForwardOptionVisible(imageNode: ImageNode): Boolean = false

    override fun isSendToOptionVisible(imageNode: ImageNode): Boolean = true

}