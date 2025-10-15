package mega.privacy.android.app.presentation.settings

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.CookieSettingsNavKey

fun EntryProviderScope<NavKey>.cookieSettingsNavigationDestination(removeDestination: () -> Unit) {
    entry<CookieSettingsNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, CookiePreferencesActivity::class.java)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}