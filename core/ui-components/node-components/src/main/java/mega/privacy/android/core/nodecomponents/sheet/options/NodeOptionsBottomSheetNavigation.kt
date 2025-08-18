package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.bottomsheet.megaBottomSheet
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data class NodeOptionsBottomSheet(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
)

@OptIn(ExperimentalMaterial3Api::class)
internal fun NavGraphBuilder.nodeOptionsBottomSheet(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    megaBottomSheet<NodeOptionsBottomSheet> {
        val args = it.toRoute<NodeOptionsBottomSheet>()

        if (args.nodeHandle == -1L) {
            navigationHandler.back()
            return@megaBottomSheet
        }

        NodeOptionsBottomSheetRoute(
            navigationHandler = navigationHandler,
            onDismiss = navigationHandler::back,
            nodeId = args.nodeHandle,
            nodeSourceType = args.nodeSourceType,
            onTransfer = onTransfer,
        )
    }
}