package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.bottomsheet.megaBottomSheet
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class NodeOptionsBottomSheetNavKey(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) : NoSessionNavKey.Optional {

    companion object {
        const val RESULT = "NodeOptionsBottomSheetNavKey:extra_result"
    }
}

sealed class NodeOptionsBottomSheetResult() {
    data class Navigation(val navKey: NavKey) :
        NodeOptionsBottomSheetResult()

    data class Transfer(val event: TransferTriggerEvent) :
        NodeOptionsBottomSheetResult()

    data class Rename(val nodeId: NodeId) :
        NodeOptionsBottomSheetResult()

    data class NodeNameCollision(val result: NodeNameCollisionsResult) :
        NodeOptionsBottomSheetResult()
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.nodeOptionsBottomSheet(
    navigationHandler: NavigationHandler,
    returnResult: (String, NodeOptionsBottomSheetResult) -> Unit,
) {
    entry<NodeOptionsBottomSheetNavKey>(metadata = bottomSheetMetadata(skipPartiallyExpanded = false)) {
        if (it.nodeHandle == -1L) {
            navigationHandler.back()
            return@entry
        }

        NodeOptionsBottomSheetRoute(
            navigationHandler = navigationHandler,
            onDismiss = navigationHandler::back,
            nodeId = it.nodeHandle,
            nodeSourceType = it.nodeSourceType,
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
        )
    }
}