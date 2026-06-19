package mega.privacy.android.shared.nodes.dialog.newfolder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.destination.NewFolderDialogNavKey

/**
 * Listens for the New Folder dialog result and, on each arrival, clears the result before
 * invoking [onFolderCreated] so back-navigation doesn't replay the previous creation.
 * Emissions are safe-cast via `as? Long` to avoid runtime `ClassCastException` on the
 * untyped `NavigationHandler.monitorResult` channel.
 */
@Composable
fun rememberNewFolderResult(
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    onFolderCreated: (NodeId) -> Unit,
) {
    val result by monitorResult(NewFolderDialogNavKey.FOLDER_HANDLE_RESULT)
        .collectAsStateWithLifecycle(null)
    val folderHandle = result as? Long

    LaunchedEffect(folderHandle) {
        val handle = folderHandle ?: return@LaunchedEffect
        clearResult(NewFolderDialogNavKey.FOLDER_HANDLE_RESULT)
        onFolderCreated(NodeId(handle))
    }
}
