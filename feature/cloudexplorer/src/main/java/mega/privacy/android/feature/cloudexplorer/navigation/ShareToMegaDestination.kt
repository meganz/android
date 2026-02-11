package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatExplorerNavKey
import mega.privacy.android.navigation.destination.CloudExplorerNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey

class ShareToMegaDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            shareToMegaDestination(
                navigate = navigationHandler::navigate,
            )
        }

    fun EntryProviderScope<NavKey>.shareToMegaDestination(
        navigate: (NavKey) -> Unit,
    ) {
        entry<ShareToMegaNavKey>(
            metadata = transparentMetadata()
        ) { key ->

            val viewModel =
                hiltViewModel<ShareToMegaViewModel, ShareToMegaViewModel.Factory> { factory ->
                    factory.create(ShareToMegaViewModel.Args(shareUris = key.shareUris))
                }

            ShareToMegaScreen(
                onNavigateToCloudExplorer = { navigate(CloudExplorerNavKey) },
                onNavigateToChatExplorer = { navigate(ChatExplorerNavKey) },
            )
        }
    }
}