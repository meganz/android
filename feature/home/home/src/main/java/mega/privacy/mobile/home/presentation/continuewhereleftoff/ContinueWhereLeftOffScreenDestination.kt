package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.ContinueWhereLeftOffScreenNavKey

fun EntryProviderScope<NavKey>.continueWhereLeftOffScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<ContinueWhereLeftOffScreenNavKey> {
        val viewModel = hiltViewModel<ContinueWhereLeftOffListViewModel>()

        ContinueWhereLeftOffListScreen(
            viewModel = viewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            onBack = navigationHandler::back,
        )
    }
}
