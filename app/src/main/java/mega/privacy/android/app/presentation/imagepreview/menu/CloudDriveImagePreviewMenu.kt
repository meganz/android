package mega.privacy.android.app.presentation.imagepreview.menu

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import javax.inject.Inject

internal class CloudDriveImagePreviewMenu @Inject constructor(
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
) : ImagePreviewMenu {
    private val backupsMutex = Mutex()
    private var cachedNodeId: Long? = null
    private var cachedInBackups: Boolean = false

    override suspend fun isInfoMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isSlideshowMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.type !is VideoFileTypeInfo
    }

    override suspend fun isFavouriteMenuVisible(imageNode: ImageNode): Boolean {
        return !isInBackups(imageNode)
    }

    override suspend fun isLabelMenuVisible(imageNode: ImageNode): Boolean {
        return !isInBackups(imageNode)
    }

    override suspend fun isDisputeMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.isTakenDown
    }

    override suspend fun isOpenWithMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isForwardMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isSaveToDeviceMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isImportMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isGetLinkMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isSendToChatMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isShareMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isRenameMenuVisible(imageNode: ImageNode): Boolean {
        return !isInBackups(imageNode)
    }

    override suspend fun isHideMenuVisible(imageNode: ImageNode): Boolean {
        return !imageNode.isMarkedSensitive
                && !imageNode.isSensitiveInherited
                && haveOwnerAccessPermission(imageNode)
                && !isInBackups(imageNode)
    }

    override suspend fun isUnhideMenuVisible(imageNode: ImageNode): Boolean {
        return imageNode.isMarkedSensitive
                && !imageNode.isSensitiveInherited
                && haveOwnerAccessPermission(imageNode)
                && !isInBackups(imageNode)
    }

    override suspend fun isMoveMenuVisible(imageNode: ImageNode): Boolean {
        return !isInBackups(imageNode)
    }

    override suspend fun isCopyMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isRestoreMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isRemoveMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isAvailableOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isRemoveOfflineMenuVisible(imageNode: ImageNode): Boolean {
        return false
    }

    override suspend fun isMoreMenuVisible(imageNode: ImageNode): Boolean {
        return true
    }

    override suspend fun isMoveToRubbishBinMenuVisible(imageNode: ImageNode): Boolean {
        return !isInBackups(imageNode)
    }

    private suspend fun haveOwnerAccessPermission(
        imageNode: ImageNode,
    ) = getNodeAccessPermission(imageNode.id)?.let { accessPermission ->
        accessPermission == AccessPermission.OWNER
    } ?: false

    private suspend fun isInBackups(imageNode: ImageNode): Boolean {
        val nodeId = imageNode.id.longValue
        return backupsMutex.withLock {
            if (cachedNodeId != nodeId) {
                cachedInBackups = runCatching { isNodeInBackupsUseCase(nodeId) }
                    .onFailure { Timber.e(it, "Failed to check if node is in backups") }
                    .getOrDefault(false)
                cachedNodeId = nodeId
            }
            cachedInBackups
        }
    }
}
