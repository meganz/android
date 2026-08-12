package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CreateAccountNavKey
import mega.privacy.android.navigation.destination.LoginNavKey
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.setPendingDeepLink
import mega.privacy.android.shared.nodes.sheet.PublicLinkAuthAlertBottomSheet
import mega.privacy.android.shared.nodes.sheet.PublicLinkType

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.searchScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<SearchNavKey> { key ->
        val viewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory> { factory ->
            factory.create(
                SearchViewModel.Args(
                    parentHandle = key.parentHandle,
                    nodeSourceType = key.nodeSourceType,
                    folderLinkUrl = key.folderLinkUrl,
                )
            )
        }
        val nodeOptionsActionSourceType = if (key.nodeSourceType == NodeSourceType.FOLDER_LINK) {
            NodeSourceType.FOLDER_LINK
        } else {
            NodeSourceType.SEARCH
        }
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(nodeOptionsActionSourceType) }
            )
        val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
        var showLoginRequiredSheet by rememberSaveable { mutableStateOf(false) }
        val activity = LocalActivity.current

        SearchScreen(
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
            viewModel = viewModel,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
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
                    activity.setPendingDeepLink(key.folderLinkUrl)
                    navigationHandler.navigate(CreateAccountNavKey())
                },
                onLoginClicked = {
                    showLoginRequiredSheet = false
                    activity.setPendingDeepLink(key.folderLinkUrl)
                    navigationHandler.navigate(LoginNavKey())
                },
                onDismissSheet = { showLoginRequiredSheet = false },
            )
        }
    }
}
