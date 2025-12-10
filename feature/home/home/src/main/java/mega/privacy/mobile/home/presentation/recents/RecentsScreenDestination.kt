package mega.privacy.mobile.home.presentation.recents

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RecentsScreenNavKey
import mega.privacy.mobile.home.presentation.recents.RecentsWidgetConstants.SCREEN_MAX_BUCKETS

fun EntryProviderScope<NavKey>.recentsScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<RecentsScreenNavKey> {
        val viewModel =
            hiltViewModel<RecentsViewModel, RecentsViewModel.Factory> { factory ->
                factory.create(maxBucketCount = SCREEN_MAX_BUCKETS)
            }
        RecentsScreen(
            viewModel = viewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            onBack = navigationHandler::back,
        )
    }
}
