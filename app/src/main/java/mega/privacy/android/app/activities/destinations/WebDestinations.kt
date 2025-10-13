package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.navigation.destination.WebSiteNavKey

fun EntryProviderBuilder<NavKey>.webDestinations(removeDestination: () -> Unit) {
    entry<WebSiteNavKey> { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.launchUrl(key.url)

            removeDestination()
        }
    }
}