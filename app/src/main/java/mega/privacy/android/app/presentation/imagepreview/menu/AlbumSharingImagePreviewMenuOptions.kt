package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.shares.AccessPermission.OWNER
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

internal class AlbumSharingImagePreviewMenuOptions @Inject constructor(
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbish: IsNodeInRubbish,
) : ImagePreviewMenuOptions {
    override suspend fun isSlideshowOptionVisible(imageNode: ImageNode): Boolean {
        return imageNode.type !is VideoFileTypeInfo
    }

    override suspend fun isGetLinkOptionVisible(imageNode: ImageNode): Boolean {
        return !imageNode.isTakenDown
                && !isNodeInRubbish(handle = imageNode.id.longValue)
                && getNodeAccessPermission(nodeId = imageNode.id) == OWNER
    }

    override suspend fun isSaveToDeviceOptionVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isForwardOptionVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isSendToOptionVisible(imageNode: ImageNode): Boolean {
        return !imageNode.isTakenDown
                && imageNode.exportedData == null
                && !isNodeInRubbish(handle = imageNode.id.longValue)
                && getNodeAccessPermission(nodeId = imageNode.id) == OWNER
    }

    override suspend fun isMoreOptionVisible(imageNode: ImageNode): Boolean {
        return false
    }
}
