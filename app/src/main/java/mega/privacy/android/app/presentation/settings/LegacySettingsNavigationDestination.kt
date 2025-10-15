package mega.privacy.android.app.presentation.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacySettingsNavKey

fun EntryProviderScope<NavKey>.legacySettingsScreen(removeDestination: () -> Unit) {
    entry<LegacySettingsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val targetPreference = key.targetPreference
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = SettingsActivity.getIntent(context, targetPreference)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

class SettingFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            legacySettingsScreen(navigationHandler::back)
        }
}

