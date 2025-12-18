package mega.privacy.mobile.home.presentation.recents.bucket

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RecentsBucketScreenNavKey

fun EntryProviderScope<NavKey>.recentsBucketScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<RecentsBucketScreenNavKey> { navKey ->
        val viewModel =
            hiltViewModel<RecentsBucketViewModel, RecentsBucketViewModel.Factory> { factory ->
                factory.create(
                    RecentsBucketViewModel.Args(
                        identifier = navKey.identifier,
                        isMediaBucket = navKey.isMediaBucket,
                        folderName = navKey.folderName,
                        nodeSourceType = navKey.nodeSourceType,
                        timestamp = navKey.timestamp,
                        fileCount = navKey.fileCount,
                    )
                )
            }
        RecentsBucketScreen(
            viewModel = viewModel,
            onNavigate = navigationHandler::navigate,
            transferHandler = transferHandler,
            onBack = navigationHandler::back,
            nodeSourceType = navKey.nodeSourceType,
        )
    }
}

