package mega.privacy.android.shared.original.core.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Launches a native Android Folder Picker
 *
 * @param onFolderSelected - Callback to be called when a folder is selected by the user
 * @param initialUri - URI to open the folder picker at. Note that this should be a content URI.
 * To open at the root directory, set it to content://com.android.externalstorage.documents/root/primary
 */
@Composable
fun launchFolderPicker(
    initialUri: Uri? = null,
    onCancel: () -> Unit = {},
    onFolderSelected: (Uri) -> Unit,
): ActivityResultLauncher<Uri?> =
    rememberLauncherForActivityResult(object : ActivityResultContracts.OpenDocumentTree() {
        override fun createIntent(context: Context, input: Uri?): Intent =
            super.createIntent(context, initialUri)
    }) { directoryUri ->
        directoryUri?.let {
            onFolderSelected(it)
        } ?: onCancel()
    }
