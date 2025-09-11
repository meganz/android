package mega.privacy.android.app.presentation.settings

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.activities.settingsActivities.CookiePreferencesActivity
import mega.privacy.android.navigation.destination.CookieSettings

fun NavGraphBuilder.cookieSettingsNavigationDestination(removeDestination: () -> Unit) {
    composable<CookieSettings> {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, CookiePreferencesActivity::class.java)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }

    }
}