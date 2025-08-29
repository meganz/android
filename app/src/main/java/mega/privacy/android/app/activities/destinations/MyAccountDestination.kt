package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.navigation.destination.MyAccount
import mega.privacy.android.navigation.megaNavigator

fun NavGraphBuilder.myAccount(removeDestination: () -> Unit) {
    composable<MyAccount> {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openMyAccountActivity(context)

            // Immediately pop this destination from the back stack
            removeDestination()
        }

    }
}
