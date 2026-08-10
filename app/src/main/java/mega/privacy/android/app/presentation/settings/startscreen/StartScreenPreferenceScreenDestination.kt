package mega.privacy.android.app.presentation.settings.startscreen

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.activities.settingsActivities.StartScreenPreferencesActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.StartScreenPreferenceNavKey

fun EntryProviderScope<NavKey>.startScreenPreferenceScreen(removeDestination: () -> Unit) {
    entry<StartScreenPreferenceNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, StartScreenPreferencesActivity::class.java)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
