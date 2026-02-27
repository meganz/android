package mega.privacy.android.app.presentation.fileexplorer

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.main.FileExplorerActivity
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.FileExplorerNavKey

fun EntryProviderScope<NavKey>.fileExplorer(
    removeDestination: (NavKey) -> Unit,
    returnResult: (String, Long?) -> Unit,
) {
    entry<FileExplorerNavKey>(metadata = transparentMetadata()) { key ->
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) exit@{ result ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode != RESULT_OK || data == null) {
                removeDestination(key)
            }

            val folderHandle = data?.getLongExtra("IMPORT_TO", 0)
            returnResult(FileExplorerNavKey.RESULT_FOLDER_HANDLE, folderHandle)
        }

        LaunchedOnceEffect {
            val intent = Intent(context, FileExplorerActivity::class.java).apply {
                action = key.action
            }

            launcher.launch(intent)
        }
    }
}