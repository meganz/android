package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data class NodeOptionsBottomSheetNavKey(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val partiallyExpand: Boolean = true,
    val chatId: Long? = null,
    val msgId: Long? = null,
    val publicLinkUrl: String? = null,
    val localFilePath: String? = null,
    val serializedData: String? = null,
) : NoSessionNavKey.Optional {

    companion object {
        const val RESULT = "NodeOptionsBottomSheetNavKey:extra_result"
    }
}

/**
 * Variant of [NodeOptionsBottomSheetNavKey] that forces the bottom sheet to render in dark theme,
 * regardless of the host Activity's theme. Used by screens that always display in dark mode
 * (e.g. the audio player).
 *
 * **Field sync**: All constructor fields must mirror [NodeOptionsBottomSheetNavKey] exactly.
 * If a field is added or removed from that class, update this class as well.
 */
@Serializable
data class DarkNodeOptionsBottomSheetNavKey(
    val nodeHandle: Long = -1L,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val partiallyExpand: Boolean = true,
    val chatId: Long? = null,
    val msgId: Long? = null,
    val publicLinkUrl: String? = null,
    val localFilePath: String? = null,
    val serializedData: String? = null,
) : NoSessionNavKey.Optional {
    companion object {
        /** Result key used by [NodeOptionsBottomSheetNavigation]; identical to [NodeOptionsBottomSheetNavKey.RESULT]. */
        const val RESULT = NodeOptionsBottomSheetNavKey.RESULT
    }
}

data class NodeOptionsBottomSheetResult(
    val action: MenuAction,
    val node: TypedNode,
)

@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.nodeOptionsBottomSheet(
    navigationHandler: NavigationHandler,
    returnResult: (String, NodeOptionsBottomSheetResult) -> Unit,
) {
    entry<NodeOptionsBottomSheetNavKey>(metadata = bottomSheetMetadata(skipPartiallyExpanded = false)) { navKey ->
        NodeOptionsBottomSheetContent(
            nodeHandle = navKey.nodeHandle,
            nodeSourceType = navKey.nodeSourceType,
            partiallyExpand = navKey.partiallyExpand,
            chatId = navKey.chatId,
            msgId = navKey.msgId,
            publicLinkUrl = navKey.publicLinkUrl,
            localFilePath = navKey.localFilePath,
            serializedData = navKey.serializedData,
            onDismiss = { navigationHandler.remove(navKey) },
            navigationHandler = navigationHandler,
            returnResult = returnResult,
        )
    }
    entry<DarkNodeOptionsBottomSheetNavKey>(
        metadata = bottomSheetMetadata(skipPartiallyExpanded = false, forceDarkTheme = true)
    ) { navKey ->
        NodeOptionsBottomSheetContent(
            nodeHandle = navKey.nodeHandle,
            nodeSourceType = navKey.nodeSourceType,
            partiallyExpand = navKey.partiallyExpand,
            chatId = navKey.chatId,
            msgId = navKey.msgId,
            publicLinkUrl = navKey.publicLinkUrl,
            localFilePath = navKey.localFilePath,
            serializedData = navKey.serializedData,
            onDismiss = { navigationHandler.remove(navKey) },
            navigationHandler = navigationHandler,
            returnResult = returnResult,
        )
    }
}

@Composable
private fun NodeOptionsBottomSheetContent(
    nodeHandle: Long,
    nodeSourceType: NodeSourceType,
    partiallyExpand: Boolean,
    chatId: Long?,
    msgId: Long?,
    publicLinkUrl: String?,
    localFilePath: String?,
    serializedData: String?,
    onDismiss: () -> Unit,
    navigationHandler: NavigationHandler,
    returnResult: (String, NodeOptionsBottomSheetResult) -> Unit,
) {
    if (nodeHandle == -1L) {
        LaunchedEffect(nodeHandle) { navigationHandler.back() }
        return
    }
    val viewModel =
        hiltViewModel<NodeOptionsBottomSheetViewModel, NodeOptionsBottomSheetViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(
                    nodeId = nodeHandle,
                    nodeSourceType = nodeSourceType,
                    partiallyExpand = partiallyExpand,
                    publicLinkUrl = publicLinkUrl,
                    localFilePath = localFilePath,
                    serializedData = serializedData,
                    chatId = chatId,
                    msgId = msgId,
                )
            }
        )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NodeOptionsBottomSheet(
        navigationHandler = navigationHandler,
        onDismiss = onDismiss,
        onActionClicked = { action, node ->
            returnResult(
                NodeOptionsBottomSheetNavKey.RESULT,
                NodeOptionsBottomSheetResult(action, node)
            )
            onDismiss()
        },
        uiState = uiState,
        onConsumeErrorState = viewModel::onConsumeErrorState,
        showSnackbar = viewModel::showSnackbar,
    )
}
