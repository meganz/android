package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.MyAccountNavKey

fun EntryProviderScope<NavKey>.myAccount(removeDestination: () -> Unit) {
    entry<MyAccountNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            MyAccountActivity.getIntent(
                context = context,
                action = key.action,
                link = key.link?.toUri()
            ).also { intent -> context.startActivity(intent) }

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}