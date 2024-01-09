package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.HasCredentials
import javax.inject.Inject

internal class FolderLinkImagePreviewMenu @Inject constructor(
    private val hasCredentials: HasCredentials,
) : ImagePreviewMenu {
    override suspend fun isInfoMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

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
        return imageNode.isTakenDown
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

    override suspend fun isImportMenuVisible(imageNode: ImageNode): Boolean {
        return hasCredentials()
    }

    override suspend fun isGetLinkMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isSendToChatMenuVisible(imageNode: ImageNode): Boolean {
        return false
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

    override suspend fun isAvailableOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isRemoveOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isMoreMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isMoveToRubbishBinMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }
}
