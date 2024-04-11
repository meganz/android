package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import javax.inject.Inject

internal class LinksImagePreviewMenu @Inject constructor() : ImagePreviewMenu {
    override suspend fun isInfoMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isSlideshowMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.type !is VideoFileTypeInfo
    }

    override suspend fun isFavouriteMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isLabelMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isDisputeMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.isTakenDown
    }

    override suspend fun isOpenWithMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isForwardMenuVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isSaveToDeviceMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isImportMenuVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isGetLinkMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isSendToChatMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isShareMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isRenameMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isHideMenuVisible(imageNode: ImageNode): Boolean {
        return !imageNode.isMarkedSensitive
    }

    override suspend fun isUnhideMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.isMarkedSensitive
    }

    override suspend fun isMoveMenuVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isCopyMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isRestoreMenuVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isRemoveMenuVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isAvailableOfflineMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isRemoveOfflineMenuVisible(imageNode: ImageNode): Boolean = false

    override suspend fun isMoreMenuVisible(imageNode: ImageNode): Boolean = true

    override suspend fun isMoveToRubbishBinMenuVisible(imageNode: ImageNode): Boolean = true
}
