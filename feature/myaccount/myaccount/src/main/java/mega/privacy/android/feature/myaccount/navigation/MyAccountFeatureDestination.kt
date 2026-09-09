package mega.privacy.android.feature.myaccount.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

/**
 * My Account feature destination.
 *
 * Registers the achievements sub-screens (info, referral bonus, invite friends, MEGA VPN/Pass free
 * trial). The achievements overview itself is served under `AchievementNavKey` by the `:app`
 * destination, which renders this module's overview view behind `ApiFeatures.ContactComposeFeature`.
 */
class MyAccountFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit =
        { navigationHandler, _ ->
            achievementsInfo()
            referralBonus()
            inviteFriends(navigationHandler)
            megaVpnFreeTrial()
            megaPassFreeTrial()
        }
}
