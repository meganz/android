package mega.privacy.android.feature.myaccount.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.MEGA_PASS_PACKAGE_NAME
import mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.openInSpecificApp
import mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.view.MegaPassFreeTrialScreen
import mega.privacy.android.navigation.destination.MegaPassFreeTrialNavKey

/**
 * Wires the [MegaPassFreeTrialScreen] for the award details carried by [MegaPassFreeTrialNavKey],
 * launching the MEGA Pass app listing when the install button is tapped.
 */
internal fun EntryProviderScope<NavKey>.megaPassFreeTrial() {
    entry<MegaPassFreeTrialNavKey> { navKey ->
        val context = LocalContext.current
        MegaPassFreeTrialScreen(
            isReceivedAward = navKey.isReceivedAward,
            storageAmount = navKey.storageAmount,
            awardStorageAmount = navKey.awardStorageAmount,
            durationInDays = navKey.durationInDays,
            awardDaysLeft = navKey.awardDaysLeft,
            onInstallButtonClicked = {
                openInSpecificApp(context, MEGA_PASS_PACKAGE_NAME)
            }
        )
    }
}
