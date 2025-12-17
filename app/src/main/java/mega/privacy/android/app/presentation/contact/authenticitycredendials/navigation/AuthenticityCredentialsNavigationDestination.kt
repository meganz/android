package mega.privacy.android.app.presentation.contact.authenticitycredendials.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AuthenticityCredentialsNavKey

/**
 * Legacy navigation destination for AuthenticityCredentialsActivity.
 */
fun EntryProviderScope<NavKey>.authenticityCredentialsLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<AuthenticityCredentialsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = AuthenticityCredentialsActivity.getIntent(
                context = context,
                email = key.email,
                isIncomingShares = key.isIncomingShares
            )
            context.startActivity(intent)
            removeDestination()
        }
    }
}

