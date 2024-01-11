package mega.privacy.android.app.presentation.node.dialogs.renamenode

internal sealed class RenameNodeDialogAction {

    data class OnLoadNodeNameDialog(val nodeName: Long) : RenameNodeDialogAction()

    data class OnRenameConfirmed(val newNodeName: String) : RenameNodeDialogAction()

    data object OnRenameValidationPassed : RenameNodeDialogAction()

    data object OnRenameSucceeded : RenameNodeDialogAction()
}