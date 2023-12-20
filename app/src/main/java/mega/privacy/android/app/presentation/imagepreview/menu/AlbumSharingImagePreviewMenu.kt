package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.shares.AccessPermission.OWNER
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

internal class AlbumSharingImagePreviewMenu @Inject constructor(
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbish: IsNodeInRubbish,
) : ImagePreviewMenu {
    override suspend fun isSlideshowMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.type !is VideoFileTypeInfo
    }

    override suspend fun isFavouriteMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isLabelMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isDisputeMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isOpenWithMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isForwardMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isSaveToDeviceMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isGetLinkMenuVisible(imageNode: ImageNode): Boolean {
        return !imageNode.isTakenDown
                && !isNodeInRubbish(handle = imageNode.id.longValue)
                && getNodeAccessPermission(nodeId = imageNode.id) == OWNER
    }

    override suspend fun isSendToChatMenuVisible(imageNode: ImageNode): Boolean {
        return !imageNode.isTakenDown
                && imageNode.exportedData == null
                && !isNodeInRubbish(handle = imageNode.id.longValue)
                && getNodeAccessPermission(nodeId = imageNode.id) == OWNER
    }

    override suspend fun isShareMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isRenameMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isMoveMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isCopyMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isRestoreMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isRemoveMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isRemoveOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isMoreMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }
}
