package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SearchNodeNavKey

fun EntryProviderBuilder<NavKey>.rubbishBin(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<RubbishBinNavKey> { key ->
        val viewModel = hiltViewModel<NewRubbishBinViewModel, NewRubbishBinViewModel.Factory>(
            creationCallback = { factory ->
                factory.create(key)
            }
        )

        RubbishBinScreen(
            viewModel = viewModel,
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
            onFolderClick = {
                navigationHandler.navigate(RubbishBinNavKey(it.longValue))
            },
            openSearch = { isFirstNavigationLevel, parentHandle ->
                navigationHandler.navigate(
                    SearchNodeNavKey(
                        isFirstNavigationLevel = isFirstNavigationLevel,
                        nodeSourceType = NodeSourceType.RUBBISH_BIN,
                        parentHandle = parentHandle
                    )
                )
            }
        )
    }
}