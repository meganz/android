package mega.privacy.android.app.presentation.videoplayer.mapper

import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerAddToAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerChatImportAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerCopyAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerDownloadAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerFileInfoAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerGetLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerHideAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerMoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRemoveLinkAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRenameAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerRubbishBinAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSaveForOfflineAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerSendToChatAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerShareAction
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerMenuAction.VideoPlayerUnhideAction
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetRootParentNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import javax.inject.Inject
import kotlin.getOrDefault

/**
 * Mapper to map the launch source to the corresponding menu actions.
 */
class LaunchSourceMapper @Inject constructor(
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val hasSensitiveInheritedUseCase: HasSensitiveInheritedUseCase,
    private val getRootParentNodeUseCase: GetRootParentNodeUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
) {

    /**
     * Maps the launch source to the corresponding menu actions.
     *
     * @param launchSource the launch source
     * @param videoNode the current playing video node
     * @param shouldShowAddTo whether the add to option should be shown
     * @param canRemoveFromChat whether the video can be removed from chat
     * @param isPaidUser whether the user is a paid user
     * @param isExpiredBusinessUser whether the business account is expired
     *
     * @return [VideoPlayerMenuAction] list
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
            add(VideoPlayerFileInfoAction)
            add(VideoPlayerShareAction)
            if (shouldShowAddTo) add(VideoPlayerAddToAction)
        }


        launchSource == RUBBISH_BIN_ADAPTER || isInRubbishBin(videoNode) ->
            if (videoNode == null) emptyList()
            else buildList {
                add(VideoPlayerFileInfoAction)
                if (shouldShowAddTo) add(VideoPlayerAddToAction)
                if (!isNodeInBackup(videoNode)) add(VideoPlayerRemoveAction)
            }


        launchSource == FROM_CHAT ->
            if (videoNode == null) emptyList()
            else buildList {
                add(VideoPlayerDownloadAction)
                add(VideoPlayerChatImportAction)
                add(VideoPlayerSaveForOfflineAction)
                if (canRemoveFromChat() && !isNodeInBackup(videoNode)) add(VideoPlayerRemoveAction)
                if (shouldShowAddTo) add(VideoPlayerAddToAction)
            }


        launchSource == FILE_LINK_ADAPTER || launchSource == ZIP_ADAPTER -> {
            buildList {
                add(VideoPlayerDownloadAction)
                add(VideoPlayerShareAction)
                if (shouldShowAddTo) add(VideoPlayerAddToAction)
            }
        }

        launchSource in listOf(FOLDER_LINK_ADAPTER, FROM_ALBUM_SHARING, VERSIONS_ADAPTER) ->
            buildList {
                add(VideoPlayerDownloadAction)
                if (shouldShowAddTo) add(VideoPlayerAddToAction)
            }

        launchSource == FROM_IMAGE_VIEWER ->
            if (videoNode == null) emptyList()
            else buildList {
                add(VideoPlayerDownloadAction)
                getHiddenNodeItem(
                    videoNode = videoNode,
                    launchSource = launchSource,
                    isPaidUser = isPaidUser,
                    isExpiredBusinessUser = isExpiredBusinessUser
                )?.let { add(it) }
                if (shouldShowAddTo) add(VideoPlayerAddToAction)
            }

        else -> {
            if (videoNode == null) emptyList()
            else {
                val nodeInBackup = isNodeInBackup(videoNode)
                val permissionLevel = getPermissionLevel(videoNode)
                buildList {
                    add(VideoPlayerDownloadAction)
                    add(VideoPlayerFileInfoAction)
                    add(VideoPlayerSendToChatAction)
                    add(VideoPlayerCopyAction)
                    if (isOwner(permissionLevel)) {
                        add(VideoPlayerShareAction)
                        if (videoNode.exportedData == null)
                            add(VideoPlayerGetLinkAction)
                        else
                            add(VideoPlayerRemoveLinkAction)
                    }
                    getHiddenNodeItem(
                        videoNode = videoNode,
                        launchSource = launchSource,
                        isPaidUser = isPaidUser,
                        isExpiredBusinessUser = isExpiredBusinessUser
                    )?.let { add(it) }
                    if (isFullAccess(permissionLevel) && !nodeInBackup) {
                        add(VideoPlayerRenameAction)
                        add(VideoPlayerMoveAction)
                    }
                    if (isRubbishBinShown(videoNode, permissionLevel) && !nodeInBackup)
                        add(VideoPlayerRubbishBinAction)
                    if (shouldShowAddTo) add(VideoPlayerAddToAction)
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

    private fun isFullAccess(permission: AccessPermission?) = runCatching {
        permission in listOf(
            AccessPermission.OWNER,
            AccessPermission.FULL
        )
    }.getOrDefault(false)

    private fun isOwner(permission: AccessPermission?) = permission == AccessPermission.OWNER

    private suspend fun hiddenNodesEnabled() =
        getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)

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
    ): VideoPlayerMenuAction? {
        if (!hiddenNodesEnabled()) return null
        if (isSharedNode(launchSource, videoNode)) return null
        if (isNodeInBackup(videoNode)) return null

        val isSensitiveInherited =
            runCatching { hasSensitiveInheritedUseCase(videoNode.id) }.getOrDefault(false)
        val canHideNode =
            !isPaidUser || isExpiredBusinessUser || (!videoNode.isMarkedSensitive && !isSensitiveInherited)
        val canUnhideNode =
            isPaidUser && !isExpiredBusinessUser && videoNode.isMarkedSensitive && !isSensitiveInherited

        return when {
            canHideNode -> VideoPlayerHideAction
            canUnhideNode -> VideoPlayerUnhideAction
            else -> null
        }
    }
}