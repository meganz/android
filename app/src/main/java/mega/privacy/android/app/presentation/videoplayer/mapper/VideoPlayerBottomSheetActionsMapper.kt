package mega.privacy.android.app.presentation.videoplayer.mapper

import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerBottomSheetAction
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetRootParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import javax.inject.Inject

/**
 * Mapper to map the launch source to the corresponding bottom sheet actions for the video player.
 */
class VideoPlayerBottomSheetActionsMapper @Inject constructor(
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val hasSensitiveInheritedUseCase: HasSensitiveInheritedUseCase,
    private val getRootParentNodeUseCase: GetRootParentNodeUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
) {

    /**
     * Maps the launch source to the corresponding bottom sheet actions.
     *
     * @param launchSource the launch source
     * @param videoNode the current playing video node
     * @param shouldShowAddTo whether the add to option should be shown
     * @param canRemoveFromChat whether the video can be removed from chat
     * @param isPaidUser whether the user is a paid user
     * @param isExpiredBusinessUser whether the business account is expired
     *
     * @return [VideoPlayerBottomSheetAction] list
     */
    suspend operator fun invoke(
        launchSource: Int,
        videoNode: TypedVideoNode?,
        shouldShowAddTo: Boolean,
        canRemoveFromChat: suspend () -> Boolean,
        isPaidUser: Boolean,
        isExpiredBusinessUser: Boolean,
    ) = when {
        launchSource == OFFLINE_ADAPTER -> buildList {
            add(VideoPlayerBottomSheetAction.FileInfo)
            add(VideoPlayerBottomSheetAction.Share)
            if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
        }

        launchSource == RUBBISH_BIN_ADAPTER || isInRubbishBin(videoNode) ->
            if (videoNode == null) emptyList()
            else buildList {
                add(VideoPlayerBottomSheetAction.FileInfo)
                if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
                if (!isNodeInBackup(videoNode)) add(VideoPlayerBottomSheetAction.Remove)
            }

        launchSource == FROM_CHAT -> buildList {
            add(VideoPlayerBottomSheetAction.Download)
            add(VideoPlayerBottomSheetAction.ChatImport)
            add(VideoPlayerBottomSheetAction.SaveForOffline)
            if (videoNode != null && canRemoveFromChat() && !isNodeInBackup(videoNode)) {
                add(VideoPlayerBottomSheetAction.Remove)
            }
            if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
        }

        launchSource == FILE_LINK_ADAPTER || launchSource == ZIP_ADAPTER -> {
            buildList {
                add(VideoPlayerBottomSheetAction.Download)
                add(VideoPlayerBottomSheetAction.Share)
                if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
            }
        }

        launchSource in listOf(FOLDER_LINK_ADAPTER, FROM_ALBUM_SHARING, VERSIONS_ADAPTER) ->
            buildList {
                add(VideoPlayerBottomSheetAction.Download)
                if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
            }

        launchSource == FROM_IMAGE_VIEWER ->
            if (videoNode == null) emptyList()
            else buildList {
                add(VideoPlayerBottomSheetAction.Download)
                getHiddenNodeItem(
                    videoNode = videoNode,
                    launchSource = launchSource,
                    isPaidUser = isPaidUser,
                    isExpiredBusinessUser = isExpiredBusinessUser
                )?.let { add(it) }
                if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
            }

        else -> {
            if (videoNode == null) emptyList()
            else {
                val nodeInBackup = isNodeInBackup(videoNode)
                val permissionLevel = getPermissionLevel(videoNode)
                buildList {
                    add(VideoPlayerBottomSheetAction.FileInfo)
                    add(VideoPlayerBottomSheetAction.Download)
                    if (isOwner(permissionLevel)) {
                        add(VideoPlayerBottomSheetAction.Share)
                        if (videoNode.exportedData == null)
                            add(VideoPlayerBottomSheetAction.GetLink)
                        else
                            add(VideoPlayerBottomSheetAction.RemoveLink)
                    }
                    add(VideoPlayerBottomSheetAction.SendToChat)
                    val isFullAccess = isFullAccess(permissionLevel) && !nodeInBackup
                    if (isFullAccess) {
                        add(VideoPlayerBottomSheetAction.Rename)
                    }
                    getHiddenNodeItem(
                        videoNode = videoNode,
                        launchSource = launchSource,
                        isPaidUser = isPaidUser,
                        isExpiredBusinessUser = isExpiredBusinessUser
                    )?.let { add(it) }
                    if (shouldShowAddTo) add(VideoPlayerBottomSheetAction.AddTo)
                    if (isFullAccess) {
                        add(VideoPlayerBottomSheetAction.Move)
                    }
                    add(VideoPlayerBottomSheetAction.Copy)
                    if (isRubbishBinShown(videoNode, permissionLevel) && !nodeInBackup)
                        add(VideoPlayerBottomSheetAction.RubbishBin)
                }
            }
        }
    }

    private suspend fun isNodeInBackup(videoNode: TypedVideoNode) =
        runCatching { isNodeInBackupsUseCase(videoNode.id.longValue) }.getOrDefault(false)

    private suspend fun isInRubbishBin(videoNode: TypedVideoNode?) = videoNode?.let {
        runCatching { isNodeInRubbishBinUseCase(videoNode.id) }.getOrDefault(false)
    } == true

    private suspend fun getPermissionLevel(videoNode: TypedVideoNode) = runCatching {
        getNodeAccessUseCase(videoNode.id)
    }.getOrNull()

    private suspend fun isRubbishBinShown(
        videoNode: TypedVideoNode,
        permission: AccessPermission?,
    ) = runCatching {
        val rubbishNode = getRubbishNodeUseCase()
        videoNode.parentId.longValue != rubbishNode?.id?.longValue && isFullAccess(permission)
    }.getOrDefault(false)

    private fun isFullAccess(permission: AccessPermission?) =
        permission == AccessPermission.OWNER || permission == AccessPermission.FULL

    private fun isOwner(permission: AccessPermission?) = permission == AccessPermission.OWNER

    private suspend fun isSharedNode(source: Int, videoNode: TypedVideoNode): Boolean {
        val isSharedSource = source in listOf(
            INCOMING_SHARES_ADAPTER,
            OUTGOING_SHARES_ADAPTER,
            LINKS_ADAPTER
        )
        return isSharedSource || isChildOfSharedNode(videoNode)
    }

    private suspend fun isChildOfSharedNode(videoNode: TypedVideoNode) =
        runCatching {
            getRootParentNodeUseCase(videoNode.id)?.isIncomingShare == true
        }.getOrDefault(false)

    private suspend fun getHiddenNodeItem(
        videoNode: TypedVideoNode,
        launchSource: Int,
        isPaidUser: Boolean,
        isExpiredBusinessUser: Boolean,
    ): VideoPlayerBottomSheetAction? {
        if (isSharedNode(launchSource, videoNode)) return null
        if (isNodeInBackup(videoNode)) return null

        val isSensitiveInherited =
            runCatching { hasSensitiveInheritedUseCase(videoNode.id) }.getOrDefault(false)
        val canHideNode =
            !isPaidUser || isExpiredBusinessUser || (!videoNode.isMarkedSensitive && !isSensitiveInherited)
        val canUnhideNode =
            isPaidUser && !isExpiredBusinessUser && videoNode.isMarkedSensitive && !isSensitiveInherited

        return when {
            canHideNode -> VideoPlayerBottomSheetAction.Hide
            canUnhideNode -> VideoPlayerBottomSheetAction.Unhide
            else -> null
        }
    }
}
