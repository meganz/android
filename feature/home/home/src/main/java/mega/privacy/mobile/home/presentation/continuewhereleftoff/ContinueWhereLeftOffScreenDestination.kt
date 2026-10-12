package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.destination.ContinueWhereLeftOffScreenNavKey
import mega.privacy.mobile.analytics.event.ContinueWhereLeftOffScreenEvent

fun EntryProviderScope<NavKey>.continueWhereLeftOffScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<ContinueWhereLeftOffScreenNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(ContinueWhereLeftOffScreenEvent)
        }
    ) {
        val viewModel = hiltViewModel<ContinueWhereLeftOffListViewModel>()

        ContinueWhereLeftOffListScreen(
            viewModel = viewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            onBack = navigationHandler::back,
        )
    }
}
