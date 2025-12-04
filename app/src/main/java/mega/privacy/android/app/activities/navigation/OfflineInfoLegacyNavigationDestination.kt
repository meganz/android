package mega.privacy.android.app.activities.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.utils.Constants.HANDLE
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.OfflineInfoNavKey

fun EntryProviderScope<NavKey>.offlineInfoScreen(
    removeDestination: () -> Unit,
) {
    entry<OfflineInfoNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, OfflineFileInfoActivity::class.java).putExtra(
                    HANDLE,
                    args.handle
                )
            )
            removeDestination()
        }
    }
}

@Deprecated(
    "Replace once offline info has been refactored",
    replaceWith = ReplaceWith("mega.privacy.android.feature.clouddrive.navigation.CloudDriveFeatureDestination")
)
class LegacyCloudDriveFeatureDestination() : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            offlineInfoScreen(navigationHandler::back)
        }

}