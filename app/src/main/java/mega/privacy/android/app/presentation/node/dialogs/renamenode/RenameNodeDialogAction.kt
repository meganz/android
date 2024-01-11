package mega.privacy.android.app.presentation.node.dialogs.renamenode

internal sealed class RenameNodeDialogAction {

    data class OnLoadNodeName(val nodeId: Long) : RenameNodeDialogAction()

    data class OnRenameConfirmed(val nodeId: Long, val newNodeName: String) : RenameNodeDialogAction()

    data object OnRenameValidationPassed : RenameNodeDialogAction()

    data object OnRenameSucceeded : RenameNodeDialogAction()
}