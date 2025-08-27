package mega.privacy.android.core.nodecomponents.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.core.nodecomponents.dialog.delete.moveToRubbishOrDeleteDialogM3
import mega.privacy.android.core.nodecomponents.dialog.removelink.removeNodeLinkDialogM3
import mega.privacy.android.core.nodecomponents.dialog.rename.renameNodeDialogM3
import mega.privacy.android.core.nodecomponents.sheet.changelabel.changeLabelBottomSheetNavigation
import mega.privacy.android.core.nodecomponents.sheet.options.nodeOptionsBottomSheet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class NodeComponentsFeatureDestination : FeatureDestination {
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            nodeOptionsBottomSheet(navigationHandler, transferHandler::setTransferEvent)
            changeLabelBottomSheetNavigation(navigationHandler::back)
            renameNodeDialogM3(navigationHandler::back)
            moveToRubbishOrDeleteDialogM3(navigationHandler::back)
            removeNodeLinkDialogM3(navigationHandler::back)
        }
}