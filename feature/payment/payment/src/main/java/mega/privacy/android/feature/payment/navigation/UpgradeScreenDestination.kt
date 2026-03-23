package mega.privacy.android.feature.payment.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

fun EntryProviderScope<NavKey>.upgradeScreens(
    navigationHandler: NavigationHandler,
) {
    entry<UpgradeAccountNavKey> { key ->
        UpgradeAccountRoute(
            isNewCreationAccount = key.isNewAccount,
            isUpgradeAccount = key.isUpgrade,
            openFromSource = key.source,
            onBack = navigationHandler::back,
        )
    }
}
