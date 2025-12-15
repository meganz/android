package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.content.destinations.fetchingContentDestination
import mega.privacy.android.app.appstate.content.destinations.homeScreens
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.feature.clouddrive.presentation.storage.overQuotaDialog
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.AchievementNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey

class MainNavigationFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            homeScreens(navigationHandler = navigationHandler, transferHandler = transferHandler)
            fetchingContentDestination()
            overQuotaDialog(
                navigateToUpgradeAccount = {
                    navigationHandler.navigate(UpgradeAccountNavKey())
                },
                navigateToCustomizedPlan = { context, email, accountType ->
                    AlertsAndWarnings.askForCustomizedPlan(
                        context = context,
                        myEmail = email,
                        accountType = accountType
                    )
                },
                navigateToAchievements = {
                    navigationHandler.navigate(
                        AchievementNavKey
                    )
                },
                onDismiss = navigationHandler::remove
            )
        }
}