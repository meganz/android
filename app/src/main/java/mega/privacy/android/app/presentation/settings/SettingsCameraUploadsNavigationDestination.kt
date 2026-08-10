package mega.privacy.android.app.presentation.settings

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey


class SettingsCameraUploadsFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            settingsCameraUploadsLegacyNavigationDestination(
                removeDestination = navigationHandler::remove
            )
        }

    fun EntryProviderScope<NavKey>.settingsCameraUploadsLegacyNavigationDestination(
        removeDestination: (NavKey) -> Unit,
    ) {
        entry<SettingsCameraUploadsNavKey>(
            metadata = transparentMetadata()
        ) { key ->
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, SettingsCameraUploadsActivity::class.java))

                // Immediately pop this destination from the back stack
                removeDestination(key)
            }
        }
    }
}

