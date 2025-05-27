package mega.privacy.android.shared.original.core.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment

/**
 * Launches a native Android Folder Picker
 *
 * @param writePermission if true will grand write permission too, only read permission otherwise. Default is true.
 * @param onFolderSelected - Callback to be called when a folder is selected by the user
 * @param initialUri - URI to open the folder picker at. Note that this should be a content URI.
 * To open at the root directory, set it to content://com.android.externalstorage.documents/root/primary
 */
@Composable
fun launchFolderPicker(
    initialUri: Uri? = null,
    writePermission: Boolean = true,
    onCancel: () -> Unit = {},
    onFolderSelected: (selectedFolderUri: Uri) -> Unit,
): ActivityResultLauncher<Uri?> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
        persistableOpenDocumentTree(initialUri, writePermission)
    ) { directoryUri ->
        onResult(directoryUri, writePermission, context, onCancel, onFolderSelected)
    }
}

/**
 * Launches a native Android Folder Picker
 *
 * @param writePermission if true will grand write permission too, only read permission otherwise. Default is true.
 * @param onFolderSelected - Callback to be called when a folder is selected by the user
 * @param initialUri - URI to open the folder picker at. Note that this should be a content URI.
 * To open at the root directory, set it to content://com.android.externalstorage.documents/root/primary
 */
fun Fragment.launchFolderPicker(
    initialUri: Uri? = null,
    writePermission: Boolean = true,
    onCancel: () -> Unit = {},
    onFolderSelected: (selectedFolderUri: Uri) -> Unit,
) = registerForActivityResult(
    persistableOpenDocumentTree(initialUri, writePermission)
) { directoryUri ->
    onResult(directoryUri, writePermission, requireContext(), onCancel, onFolderSelected)
}

private fun onResult(
    directoryUri: Uri?,
    writePermission: Boolean,
    context: Context,
    onCancel: () -> Unit = {},
    onFolderSelected: (Uri) -> Unit,
) {
    directoryUri?.let {
        context.contentResolver.takePersistableUriPermission(
            directoryUri,
            if (writePermission) Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION else Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        onFolderSelected(it)
    } ?: onCancel()
}

private fun persistableOpenDocumentTree(
    initialUri: Uri?,
    writePermission: Boolean,
) = object : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        super.createIntent(context, initialUri).also {
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (writePermission) {
                it.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
}
