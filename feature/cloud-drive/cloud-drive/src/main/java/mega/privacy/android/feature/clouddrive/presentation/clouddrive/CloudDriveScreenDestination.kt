package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey

fun EntryProviderBuilder<NavKey>.cloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    openSearch: (Boolean, Long, NodeSourceType) -> Unit,
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
            openSearch = openSearch,
        )
    }
}