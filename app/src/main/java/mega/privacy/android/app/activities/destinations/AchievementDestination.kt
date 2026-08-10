package mega.privacy.android.app.activities.destinations

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AchievementNavKey

fun EntryProviderScope<NavKey>.achievement(removeDestination: () -> Unit) {
    entry<AchievementNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, AchievementsFeatureActivity::class.java))
            removeDestination()
        }
    }
}
