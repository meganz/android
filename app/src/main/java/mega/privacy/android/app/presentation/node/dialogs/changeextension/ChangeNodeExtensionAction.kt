package mega.privacy.android.app.presentation.node.dialogs.changeextension

internal sealed interface ChangeNodeExtensionAction {

    data class OnChangeExtensionConfirmed(
        val nodeId: Long,
        val newNodeName: String,
    ) : ChangeNodeExtensionAction
}