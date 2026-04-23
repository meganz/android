package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.CreateAccountNavKey
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFolderLinkNavKey
import mega.privacy.android.navigation.destination.LoginNavKey
import mega.privacy.android.shared.ads.rewarded.rememberRewardedAdGate
import mega.privacy.android.shared.nodes.sheet.PublicLinkAuthAlertBottomSheet
import mega.privacy.android.shared.nodes.sheet.PublicLinkType

@OptIn(ExperimentalMaterial3Api::class)
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
                onDeferredAction = { _, action -> rewardedAdGate.requestAction(action) },
            )
            val selectionModeActionHandler = rememberMultiNodeActionHandler(
                viewModel = nodeOptionsActionViewModel,
                navigationHandler = navigationHandler,
                onDeferredAction = { _, action -> rewardedAdGate.requestAction(action) },
            )
            val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
            var showLoginRequiredSheet by remember { mutableStateOf(false) }

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

            EventEffect(
                event = nodeActionState.loginRequiredEvent,
                onConsumed = nodeOptionsActionViewModel::resetLoginRequiredEvent,
            ) {
                showLoginRequiredSheet = true
            }


            if (showLoginRequiredSheet) {
                PublicLinkAuthAlertBottomSheet(
                    type = PublicLinkType.Folder,
                    onSignupClicked = {
                        showLoginRequiredSheet = false
                        navigationHandler.navigate(CreateAccountNavKey())
                    },
                    onLoginClicked = {
                        showLoginRequiredSheet = false
                        navigationHandler.navigate(LoginNavKey())
                    },
                    onDismissSheet = { showLoginRequiredSheet = false },
                )
            }
        }
    }
}
