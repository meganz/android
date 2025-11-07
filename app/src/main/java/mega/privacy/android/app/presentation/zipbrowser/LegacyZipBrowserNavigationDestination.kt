package mega.privacy.android.app.presentation.zipbrowser

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants.EXTRA_HANDLE_ZIP
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyFileExplorerNavKey
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey

fun EntryProviderScope<NavKey>.legacyZipBrowserScreen(removeDestination: () -> Unit) {
    entry<LegacyZipBrowserNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, ZipBrowserComposeActivity::class.java).apply {
                putExtra(EXTRA_PATH_ZIP, key.zipFilePath)
            }
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

class ZipBrowserFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            legacyZipBrowserScreen(navigationHandler::back)
        }
}

