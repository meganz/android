package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.navigation.destination.WebSite

fun NavGraphBuilder.webDestinations(removeDestination: () -> Unit) {
    composable<WebSite> {
        val url = it.toRoute<WebSite>().url
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.launchUrl(url)

            removeDestination()
        }
    }
}