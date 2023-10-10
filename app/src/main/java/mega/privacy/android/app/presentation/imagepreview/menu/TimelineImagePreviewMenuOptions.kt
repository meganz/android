package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.node.ImageNode
import javax.inject.Inject

class TimelineImagePreviewMenuOptions @Inject constructor() : ImagePreviewMenuOptions {

    override fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean = true

    override fun isLinkOptionVisible(imageNode: ImageNode): Boolean = true

    override fun isDownloadOptionVisible(imageNode: ImageNode): Boolean = true

    override fun isForwardOptionVisible(imageNode: ImageNode): Boolean = true

}