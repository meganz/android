package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogResult
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class NodeOptionsBottomSheetNavKey(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val partiallyExpand: Boolean = true,
) : NoSessionNavKey.Optional {

    companion object {
        const val RESULT = "NodeOptionsBottomSheetNavKey:extra_result"
    }
}

sealed class NodeOptionsBottomSheetResult() {
    data class Navigation(val navKey: NavKey) : NodeOptionsBottomSheetResult()

    data class Transfer(val event: TransferTriggerEvent) : NodeOptionsBottomSheetResult()

    data class Rename(val nodeId: NodeId) : NodeOptionsBottomSheetResult()

    data class NodeNameCollision(val result: NodeNameCollisionsResult) :
        NodeOptionsBottomSheetResult()

    data class RestoreSuccess(val data: RestoreData) : NodeOptionsBottomSheetResult() {
        data class RestoreData(
            val message: String,
            val parentHandle: Long,
            val restoredNodeHandle: Long,
        )
    }

    data class AddToPlaylist(val result: AddVideoToPlaylistResult) : NodeOptionsBottomSheetResult()
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.nodeOptionsBottomSheet(
    navigationHandler: NavigationHandler,
    shareFolderDialogResult: (String) -> Flow<ShareFolderDialogResult?>,
    returnResult: (String, NodeOptionsBottomSheetResult) -> Unit,
) {
    entry<NodeOptionsBottomSheetNavKey>(metadata = bottomSheetMetadata(skipPartiallyExpanded = false)) {
        if (it.nodeHandle == -1L) {
            navigationHandler.back()
            return@entry
        }
        val nodeOptionsActionViewModel: NodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { factory -> factory.create(it.nodeSourceType) }
            )
        val shareFolderResult by shareFolderDialogResult(ShareFolderDialogNavKey.RESULT)
            .collectAsStateWithLifecycle(null)

        NodeOptionsBottomSheetRoute(
            navigationHandler = navigationHandler,
            actionHandler = rememberSingleNodeActionHandler(
                navigationHandler = navigationHandler,
                viewModel = nodeOptionsActionViewModel,
            ),
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onDismiss = { navigationHandler.remove(it) },
            nodeId = it.nodeHandle,
            nodeSourceType = it.nodeSourceType,
            partiallyExpand = it.partiallyExpand,
            onTransfer = { event ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult.Transfer(event)
                )
            },
            onNavigate = { navKey ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult.Navigation(navKey)
                )
            },
            onRename = { nodeId ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult.Rename(nodeId)
                )
            },
            onCollisionResult = { result ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult.NodeNameCollision(result)
                )
            },
            onRestoreSuccess = { data ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult.RestoreSuccess(data)
                )
            },
            onAddVideoToPlaylistResult = { result ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult.AddToPlaylist(result)
                )
            },
            shareFolderDialogResult = shareFolderResult,
        )
    }
}