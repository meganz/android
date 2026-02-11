package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.cloudexplorer.CloudExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.cloudexplorer.CloudExplorerViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatExplorerNavKey
import mega.privacy.android.navigation.destination.CloudExplorerNavKey

class CloudExplorerFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            cloudExplorerDestination()
            chatExplorerDestination()
        }

    fun EntryProviderScope<NavKey>.cloudExplorerDestination() {
        entry<CloudExplorerNavKey>(
            metadata = transparentMetadata()
        ) { key ->
            val viewModel = hiltViewModel<CloudExplorerViewModel>()

            CloudExplorerScreen()
        }
    }

    fun EntryProviderScope<NavKey>.chatExplorerDestination() {
        entry<ChatExplorerNavKey>(
            metadata = transparentMetadata()
        ) { key ->
            val viewModel = hiltViewModel<ChatExplorerViewModel>()

            ChatExplorerScreen()
        }
    }
}
