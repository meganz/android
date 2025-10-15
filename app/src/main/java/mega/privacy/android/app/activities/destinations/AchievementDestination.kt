package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderBuilder<NavKey>.achievement(removeDestination: () -> Unit) {
    entry<AchievementNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openAchievements(context)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
