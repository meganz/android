package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RubbishBin
import mega.privacy.android.navigation.destination.SearchNode

fun NavGraphBuilder.rubbishBin(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    composable<RubbishBin> {
        RubbishBinScreen(
            navigationHandler = navigationHandler,
            onTransfer = transferHandler::setTransferEvent,
            onFolderClick = {
                navigationHandler.navigate(RubbishBin(it.longValue))
            },
            openSearch = { isFirstNavigationLevel, parentHandle ->
                navigationHandler.navigate(
                    SearchNode(
                        isFirstNavigationLevel = isFirstNavigationLevel,
                        nodeSourceType = NodeSourceType.RUBBISH_BIN,
                        parentHandle = parentHandle
                    )
                )
            }
        )
    }
}
