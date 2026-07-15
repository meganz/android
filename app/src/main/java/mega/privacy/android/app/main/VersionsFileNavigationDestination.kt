package mega.privacy.android.app.main

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.VersionsFileNavKey

fun EntryProviderScope<NavKey>.versionsFileScreen(
    removeDestination: () -> Unit,
) {
    entry<VersionsFileNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, VersionsFileActivity::class.java)
                    .putExtra(Constants.HANDLE, key.nodeHandle)
            )
            removeDestination()
        }
    }
}
