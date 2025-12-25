package mega.privacy.android.core.nodecomponents.dialog.leaveshare

import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper

@Serializable
data class LeaveShareDialogNavKey(val handles: String) : NavKey

fun EntryProviderScope<NavKey>.leaveShareDialogM3(
    onBack: () -> Unit,
) {
    entry<LeaveShareDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Leave Share Dialog"
            )
        )
    ) { key ->
        val mapper = remember { NodeHandlesToJsonMapper() }
        val handles = remember(key.handles) { mapper(key.handles) }

        LeaveShareDialogM3(handles = handles, onDismiss = onBack)
    }
}