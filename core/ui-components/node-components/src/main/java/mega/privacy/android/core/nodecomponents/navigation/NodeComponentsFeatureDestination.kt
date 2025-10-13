package mega.privacy.android.core.nodecomponents.navigation

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.dialog.contact.cannotVerifyContactDialogM3
import mega.privacy.android.core.nodecomponents.dialog.delete.moveToRubbishOrDeleteDialogM3
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.leaveShareDialogM3
import mega.privacy.android.core.nodecomponents.dialog.removelink.removeNodeLinkDialogM3
import mega.privacy.android.core.nodecomponents.dialog.removeshare.removeShareFolderDialogM3
import mega.privacy.android.core.nodecomponents.dialog.rename.renameNodeDialogM3
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.shareFolderAccessDialogM3
import mega.privacy.android.core.nodecomponents.sheet.changelabel.changeLabelBottomSheetNavigation
import mega.privacy.android.core.nodecomponents.sheet.options.nodeOptionsBottomSheet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class NodeComponentsFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderBuilder<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            renameNodeDialogM3(navigationHandler::back)
            moveToRubbishOrDeleteDialogM3(navigationHandler::back)
            removeNodeLinkDialogM3(navigationHandler::back)
            removeShareFolderDialogM3(navigationHandler::back)
            cannotVerifyContactDialogM3(navigationHandler::back)
            leaveShareDialogM3(navigationHandler::back)
            shareFolderAccessDialogM3(navigationHandler::back)
            nodeOptionsBottomSheet(navigationHandler, transferHandler::setTransferEvent)
            changeLabelBottomSheetNavigation(navigationHandler::back)
        }
}