package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.bottomsheet.megaBottomSheet

@Serializable
data class NodeOptionsBottomSheetNavKey(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
internal fun NavGraphBuilder.nodeOptionsBottomSheet(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    megaBottomSheet<NodeOptionsBottomSheetNavKey> {
        val args = it.toRoute<NodeOptionsBottomSheetNavKey>()

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

@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderBuilder<NavKey>.nodeOptionsBottomSheet(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<NodeOptionsBottomSheetNavKey>(metadata = bottomSheetMetadata()) {

        if (it.nodeHandle == -1L) {
            navigationHandler.back()
            return@entry
        }

        NodeOptionsBottomSheetRoute(
            navigationHandler = navigationHandler,
            onDismiss = navigationHandler::back,
            nodeId = it.nodeHandle,
            nodeSourceType = it.nodeSourceType,
            onTransfer = onTransfer,
        )
    }
}