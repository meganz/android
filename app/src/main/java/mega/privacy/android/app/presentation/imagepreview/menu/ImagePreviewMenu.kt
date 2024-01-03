package mega.privacy.android.app.presentation.imagepreview.menu

import mega.privacy.android.domain.entity.node.ImageNode

interface ImagePreviewMenu {
    suspend fun isInfoMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isSlideshowMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isFavouriteMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isLabelMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isDisputeMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isOpenWithMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isForwardMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isSaveToDeviceMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isImportMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isGetLinkMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isSendToChatMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isShareMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isRenameMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isMoveMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isCopyMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isRestoreMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isRemoveMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isAvailableOfflineMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isRemoveOfflineMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isMoreMenuVisible(imageNode: ImageNode): Boolean

    suspend fun isMoveToRubbishBinMenuVisible(imageNode: ImageNode): Boolean
}
