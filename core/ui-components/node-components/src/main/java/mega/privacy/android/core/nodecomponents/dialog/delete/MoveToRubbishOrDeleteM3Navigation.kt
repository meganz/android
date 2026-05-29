package mega.privacy.android.core.nodecomponents.dialog.delete

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable

/**
 * Navigation arguments for the move to rubbish or delete dialog
 *
 * @property isInRubbish Whether the nodes are already in rubbish bin
 * @property nodeHandles List of node handles to process
 */
@Serializable
data class MoveToRubbishOrDeleteDialogArgs(
    val isInRubbish: Boolean,
    val nodeHandles: List<Long>
) : NavKey {
    companion object {
        /** Result key published when the user confirms the positive action. */
        const val RESULT = "MoveToRubbishOrDeleteDialogArgs:result"
    }
}

/**
 * Result published by [MoveToRubbishOrDeleteNodeDialogM3] when the user clicks the
 * positive button. [isInRubbish] mirrors the dialog mode so subscribers can tell
 * apart a confirmed Move-to-Rubbish from a confirmed Delete-Permanently.
 */
@Serializable
data class MoveToRubbishOrDeleteDialogResult(val isInRubbish: Boolean)

/**
 * Navigation function to add the move to rubbish or delete dialog to the navigation graph
 *
 * @param onBack Callback when the dialog is dismissed (cancel, back press, or post-confirm pop)
 * @param returnResult Callback to publish [MoveToRubbishOrDeleteDialogResult] when the user
 *   confirms; subscribers monitor [MoveToRubbishOrDeleteDialogArgs.RESULT].
 */
internal fun EntryProviderScope<NavKey>.moveToRubbishOrDeleteDialogM3(
    onBack: () -> Unit,
    returnResult: (String, MoveToRubbishOrDeleteDialogResult) -> Unit,
) {
    entry<MoveToRubbishOrDeleteDialogArgs>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Move to Rubbish or Delete Dialog"
            )
        )
    ) { key ->
        MoveToRubbishOrDeleteNodeDialogM3(
            nodes = key.nodeHandles,
            isNodeInRubbish = key.isInRubbish,
            onDismiss = onBack,
            onConfirm = {
                returnResult(
                    MoveToRubbishOrDeleteDialogArgs.RESULT,
                    MoveToRubbishOrDeleteDialogResult(isInRubbish = key.isInRubbish),
                )
                onBack()
            },
        )
    }
}
