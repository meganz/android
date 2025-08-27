package mega.privacy.android.core.nodecomponents.dialog.delete

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
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
internal fun NavGraphBuilder.moveToRubbishOrDeleteDialogM3(
    onBack: () -> Unit,
) {
    dialog<MoveToRubbishOrDeleteDialogArgs> {
        val args = it.toRoute<MoveToRubbishOrDeleteDialogArgs>()

        MoveToRubbishOrDeleteNodeDialogM3(
            nodes = args.nodeHandles,
            isNodeInRubbish = args.isInRubbish,
            onDismiss = onBack
        )
    }
}