package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SearchNodeNavKey

fun NavGraphBuilder.rubbishBin(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    composable<RubbishBinNavKey> {
        RubbishBinScreen(
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
