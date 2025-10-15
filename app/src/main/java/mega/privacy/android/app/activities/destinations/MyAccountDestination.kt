package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderBuilder<NavKey>.myAccount(removeDestination: () -> Unit) {
    entry<MyAccountNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openMyAccountActivity(context)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
