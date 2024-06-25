package mega.privacy.android.shared.original.core.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Launches a native Android Folder Picker
 *
 * @param onFolderSelected - Callback to be called when a folder is selected by the user
 */
@Composable
fun launchFolderPicker(
    onCancel: () -> Unit = {},
    onFolderSelected: (Uri) -> Unit,
): ActivityResultLauncher<Uri?> =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { directoryUri ->
        directoryUri?.let {
            onFolderSelected(it)
        } ?: onCancel()
    }
