package mega.privacy.android.app.main.dialog.newfolder

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.NewFolderDialogNavKey
import mega.privacy.android.shared.nodes.dialog.newfolder.NewFolderNodeDialog

data object NewFolderDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<in DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            newFolderDialogDestination(
                remove = navigationHandler::remove,
                returnResult = navigationHandler::returnResult,
                onDialogHandled = onHandled,
            )
        }
}

fun EntryProviderScope<in DialogNavKey>.newFolderDialogDestination(
    remove: (NavKey) -> Unit,
    returnResult: (String, Long) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<NewFolderDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        NewFolderNodeDialog(
            parentNode = key.parentNodeId,
            onCreateFolder = { folderId ->
                if (folderId != null) {
                    returnResult(NewFolderDialogNavKey.FOLDER_HANDLE_RESULT, folderId.longValue)
                } else {
                    remove(key)
                }
                onDialogHandled()
            },
            onDismiss = {
                remove(key)
                onDialogHandled()
            },
        )
    }
}
