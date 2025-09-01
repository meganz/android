package mega.privacy.android.core.nodecomponents.dialog.leaveshare

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper

@Serializable
data class LeaveShareDialogArgs(val handles: String) : NavKey

fun NavGraphBuilder.leaveShareDialogM3(
    onBack: () -> Unit,
) {
    dialog<LeaveShareDialogArgs> {
        val args = it.toRoute<LeaveShareDialogArgs>()
        val mapper = NodeHandlesToJsonMapper()
        val handles = mapper(args.handles)

        LeaveShareDialogM3(handles = handles, onDismiss = onBack)
    }
}