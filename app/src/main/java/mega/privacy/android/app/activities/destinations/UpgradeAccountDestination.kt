package mega.privacy.android.app.activities.destinations

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

fun EntryProviderScope<NavKey>.upgradeAccount(removeDestination: () -> Unit) {
    entry<UpgradeAccountNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            if (key.isUpgrade) {
                ChooseAccountActivity.navigateToUpgradeAccount(
                    context = context, source = key.source
                )
            } else {
                ChooseAccountActivity.navigateToChooseAccount(
                    context = context,
                    isNewCreationAccount = key.isNewAccount,
                )
            }

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
