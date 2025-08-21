package mega.privacy.android.core.nodecomponents.dialog.rename

sealed class RenameNodeDialogAction {

    data class OnLoadNodeName(val nodeId: Long) : RenameNodeDialogAction()

    data class OnRenameConfirmed(val nodeId: Long, val newNodeName: String) : RenameNodeDialogAction()

    data object OnRenameValidationPassed : RenameNodeDialogAction()

    data object OnChangeNodeExtensionDialogShown : RenameNodeDialogAction()
}