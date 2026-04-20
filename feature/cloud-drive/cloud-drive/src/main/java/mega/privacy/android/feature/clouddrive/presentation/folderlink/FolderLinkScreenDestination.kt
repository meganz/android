package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFolderLinkNavKey
import mega.privacy.android.shared.ads.rewarded.rememberRewardedAdGate

fun EntryProviderScope<NavKey>.folderLinkScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<FolderLinkNavKey> { key ->
        FeatureFlagGate(
            feature = AppFeatures.FolderLinkRevamp,
            disabled = {
                LaunchedEffect(Unit) {
                    navigationHandler.remove(key)
                    navigationHandler.navigate(LegacyFolderLinkNavKey(key.uriString))
                }
            }
        ) {
            val viewModel =
                hiltViewModel<FolderLinkViewModel, FolderLinkViewModel.Factory> { factory ->
                    factory.create(FolderLinkViewModel.Args(uriString = key.uriString))
                }
            val nodeOptionsActionViewModel =
                hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                    creationCallback = { it.create(NodeSourceType.FOLDER_LINK) }
                )
            val rewardedAdGate = rememberRewardedAdGate(
                onNavigate = navigationHandler::navigate,
            )
            val singleNodeActionHandler = rememberSingleNodeActionHandler(
                viewModel = nodeOptionsActionViewModel,
                navigationHandler = navigationHandler,
                onDeferredAction = rewardedAdGate::requestAction,
            )
            val selectionModeActionHandler = rememberMultiNodeActionHandler(
                viewModel = nodeOptionsActionViewModel,
                navigationHandler = navigationHandler,
                onDeferredAction = rewardedAdGate::requestAction,
            )
            FolderLinkScreen(
                viewModel = viewModel,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                navigationHandler = navigationHandler,
                singleNodeActionHandler = singleNodeActionHandler,
                selectionModeActionHandler = selectionModeActionHandler,
                rewardedAdGate = rewardedAdGate,
                onBack = navigationHandler::back,
                onNavigate = navigationHandler::navigate,
                onTransfer = transferHandler::setTransferEvent,
            )
            HandleNodeOptionsActionResult(
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                navigationHandler = navigationHandler,
                nodeActionHandler = singleNodeActionHandler,
                onTransfer = transferHandler::setTransferEvent,
            )
        }
    }
}
