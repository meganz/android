package mega.privacy.android.feature.clouddrive.presentation.rubbishbin

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.RubbishBin

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
            }
        )
    }
}
