package mega.privacy.android.feature.payment.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeRoute
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression
import mega.privacy.android.navigation.destination.QuotaWarningUpgradeNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

fun EntryProviderScope<NavKey>.upgradeScreens(
    navigationHandler: NavigationHandler,
) {
    entry<UpgradeAccountNavKey>(
        metadata = buildMetadata {
            withOverlaySuppression()
        }
    ) { key ->
        UpgradeAccountRoute(
            isNewCreationAccount = key.isNewAccount,
            isUpgradeAccount = key.isUpgrade,
            openFromSource = key.source,
            onBack = navigationHandler::back,
        )
    }

    entry<QuotaWarningUpgradeNavKey> { key ->
        QuotaWarningUpgradeRoute(
            type = key.type,
            trigger = key.trigger,
            onViewAllPlans = {
                navigationHandler.navigate(
                    UpgradeAccountNavKey(isUpgrade = true)
                )
            },
            onBack = navigationHandler::back,
        )
    }
}
