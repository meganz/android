package mega.privacy.android.feature.myaccount.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.MEGA_VPN_PACKAGE_NAME
import mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.openInSpecificApp
import mega.privacy.android.feature.myaccount.presentation.achievements.freetrial.view.MegaVPNFreeTrialScreen
import mega.privacy.android.navigation.destination.MegaVpnFreeTrialNavKey

/**
 * Wires the [MegaVPNFreeTrialScreen] for the award details carried by [MegaVpnFreeTrialNavKey],
 * launching the MEGA VPN app listing when the install button is tapped.
 */
internal fun EntryProviderScope<NavKey>.megaVpnFreeTrial() {
    entry<MegaVpnFreeTrialNavKey> { navKey ->
        val context = LocalContext.current
        MegaVPNFreeTrialScreen(
            isReceivedAward = navKey.isReceivedAward,
            storageAmount = navKey.storageAmount,
            awardStorageAmount = navKey.awardStorageAmount,
            durationInDays = navKey.durationInDays,
            awardDaysLeft = navKey.awardDaysLeft,
            onInstallButtonClicked = {
                openInSpecificApp(context, MEGA_VPN_PACKAGE_NAME)
            }
        )
    }
}
