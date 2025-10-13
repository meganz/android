package mega.privacy.android.core.nodecomponents.dialog.delete

import androidx.navigation3.runtime.EntryProviderBuilder
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
) : NavKey

/**
 * Navigation function to add the move to rubbish or delete dialog to the navigation graph
 *
 * @param onBack Callback when navigating back
 */
internal fun EntryProviderBuilder<NavKey>.moveToRubbishOrDeleteDialogM3(
    onBack: () -> Unit,
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
            onDismiss = onBack
        )
    }
}