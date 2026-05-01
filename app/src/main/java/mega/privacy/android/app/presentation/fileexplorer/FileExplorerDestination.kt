package mega.privacy.android.app.presentation.fileexplorer

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.FileExplorerNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey

fun EntryProviderScope<NavKey>.fileExplorer(
    removeDestination: (NavKey) -> Unit,
    returnResult: (String, Long?) -> Unit,
    navigationHandler: NavigationHandler,
) {
    entry<FileExplorerNavKey>(metadata = transparentMetadata()) { key ->
        if (key.shareUri != null) {
            FileExplorerShareGate(
                key = key,
                navigationHandler = navigationHandler,
                removeDestination = removeDestination,
            )
        } else {
            FileExplorerFolderPick(
                key = key,
                removeDestination = removeDestination,
                returnResult = returnResult,
            )
        }
    }
}

@Composable
private fun EntryProviderScope<NavKey>.FileExplorerShareGate(
    key: FileExplorerNavKey,
    navigationHandler: NavigationHandler,
    removeDestination: (NavKey) -> Unit,
) {
    val context = LocalContext.current
    FeatureFlagGate(
        feature = AppFeatures.FileExplorer,
        disabled = {
            LaunchedOnceEffect {
                val intent = Intent(context, FileExplorerActivity::class.java).apply {
                    action = key.action
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(key.shareUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    key.mimeType?.let { type = it }
                }
                context.startActivity(intent)
                removeDestination(key)
            }
        },
    ) {
        LaunchedEffect(Unit) {
            removeDestination(key)
            navigationHandler.navigate(
                ShareToMegaNavKey(listOf(UriPath(key.shareUri!!)))
            )
        }
    }
}

@Composable
private fun FileExplorerFolderPick(
    key: FileExplorerNavKey,
    removeDestination: (NavKey) -> Unit,
    returnResult: (String, Long?) -> Unit,
) {
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
