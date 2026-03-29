package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SearchNavKey

fun EntryProviderScope<NavKey>.searchScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<SearchNavKey> { key ->
        FeatureFlagGate(
            feature = AppFeatures.SearchRevamp,
            disabled = {
//                Legacy navigation requires a reference to Search Activity in the app module, using the legacy key instead
                LaunchedEffect(Unit) {
                    navigationHandler.remove(key)
                    navigationHandler.navigate(
                        LegacySearchNavKey(
                            key.nodeSourceType,
                            key.parentHandle
                        )
                    )
                }
            }
        )
        {
            val viewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory> { factory ->
                factory.create(
                    SearchViewModel.Args(
                        parentHandle = key.parentHandle,
                        nodeSourceType = key.nodeSourceType,
                    )
                )
            }
            val nodeOptionsActionViewModel =
                hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                    creationCallback = { it.create(NodeSourceType.SEARCH) }
                )

            SearchScreen(
                navigationHandler = navigationHandler,
                onTransfer = onTransfer,
                viewModel = viewModel,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel
            )

            HandleNodeOptionsActionResult(
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                onNavigate = navigationHandler::navigate,
                onTransfer = onTransfer,
                nodeResultFlow = navigationHandler::monitorResult,
                clearResultFlow = navigationHandler::clearResult,
            )
        }
    }
}

