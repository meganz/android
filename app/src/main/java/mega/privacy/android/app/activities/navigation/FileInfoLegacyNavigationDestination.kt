package mega.privacy.android.app.activities.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.FileInfoNavKey

/**
 * Navigation destination for FileInfo screen.
 *
 * Note: FileInfoActivity uses FileInfoScreen (Compose) internally, but there is no standalone
 * Compose Fragment or composable that can be used directly in the navigation graph.
 * Therefore, we launch the Activity which wraps the Compose screen.
 * If a standalone Compose implementation becomes available in the future (e.g., FileInfoComposeFragment),
 * this should be updated to use it directly instead of launching the Activity.
 */
fun EntryProviderScope<NavKey>.fileInfoScreen(
    removeDestination: () -> Unit,
) {
    entry<FileInfoNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, FileInfoActivity::class.java).putExtra(
                    HANDLE,
                    args.handle
                )
            )
            removeDestination()
        }
    }
}
