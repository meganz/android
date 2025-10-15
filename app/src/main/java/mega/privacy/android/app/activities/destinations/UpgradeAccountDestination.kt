package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.payment.UpgradeAccountNavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.megaNavigator

fun EntryProviderScope<NavKey>.upgradeAccount(removeDestination: () -> Unit) {
    entry<UpgradeAccountNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.megaNavigator.openUpgradeAccount(
                context = context,
                source = key.source
            )

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

