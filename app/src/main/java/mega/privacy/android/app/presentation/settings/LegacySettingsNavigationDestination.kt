package mega.privacy.android.app.presentation.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.LegacySettingsNavKey
import mega.privacy.android.navigation.extensions.typeMapOf
import mega.privacy.android.navigation.settings.arguments.TargetPreference

fun NavGraphBuilder.legacySettingsScreen(removeDestination: () -> Unit) {
    composable<LegacySettingsNavKey>(
        typeMap = mapOf(typeMapOf<TargetPreference?>())
    ) {
        val targetPreference = it.toRoute<LegacySettingsNavKey>().targetPreference
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
    override val navigationGraph: NavGraphBuilder.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            legacySettingsScreen(navigationHandler::back)
        }
}

