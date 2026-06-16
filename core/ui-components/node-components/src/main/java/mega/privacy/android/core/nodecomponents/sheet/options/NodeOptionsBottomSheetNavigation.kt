package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.material3.ExperimentalMaterial3Api
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
    val publicLinkUrl: String? = null,
    val localFilePath: String? = null,
    val serializedData: String? = null,
) : NoSessionNavKey.Optional {

    companion object {
        const val RESULT = "NodeOptionsBottomSheetNavKey:extra_result"
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
    entry<NodeOptionsBottomSheetNavKey>(metadata = bottomSheetMetadata(skipPartiallyExpanded = false)) {
        if (it.nodeHandle == -1L) {
            navigationHandler.back()
            return@entry
        }
        val viewModel =
            hiltViewModel<NodeOptionsBottomSheetViewModel, NodeOptionsBottomSheetViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(
                        nodeId = it.nodeHandle,
                        nodeSourceType = it.nodeSourceType,
                        partiallyExpand = it.partiallyExpand,
                        publicLinkUrl = it.publicLinkUrl,
                        localFilePath = it.localFilePath,
                        serializedData = it.serializedData,
                    )
                }
            )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NodeOptionsBottomSheet(
            navigationHandler = navigationHandler,
            onDismiss = { navigationHandler.remove(it) },
            onActionClicked = { action, node ->
                returnResult(
                    NodeOptionsBottomSheetNavKey.RESULT,
                    NodeOptionsBottomSheetResult(action, node)
                )
                navigationHandler.remove(it)
            },
            uiState = uiState,
            onConsumeErrorState = viewModel::onConsumeErrorState,
            showSnackbar = viewModel::showSnackbar,
        )
    }
}
