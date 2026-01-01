package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey

/**
 * Entry for Cloud Drive Screen
 * @param navigationHandler Navigation handler to handle navigation actions
 * @param onBack Callback to be invoked when the back button is pressed
 * @param onTransfer Callback to handle transfer events
 * @param setNavigationBarVisibility Optional callback to set the visibility of the bottom navigation bar, used in HomeScreens
 */
fun EntryProviderScope<NavKey>.cloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    setNavigationBarVisibility: (Boolean) -> Unit = { },
) {
    entry<CloudDriveNavKey> { key ->
        val viewModel = hiltViewModel<CloudDriveViewModel, CloudDriveViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(key)
            }
        )
        val snackbarHostState = LocalSnackBarHostState.current
        val context = LocalContext.current
        var hasShownSnackbar by rememberSaveable { mutableStateOf(false) }
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        LaunchedEffect(key.isNewFolder) {
            if (key.isNewFolder && !hasShownSnackbar) {
                snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.context_folder_created))
                hasShownSnackbar = true
            }
        }

        CloudDriveScreen(
            navigationHandler = navigationHandler,
            viewModel = viewModel,
            onBack = onBack,
            onTransfer = onTransfer,
            setNavigationBarVisibility = setNavigationBarVisibility,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel
        )
    }
}