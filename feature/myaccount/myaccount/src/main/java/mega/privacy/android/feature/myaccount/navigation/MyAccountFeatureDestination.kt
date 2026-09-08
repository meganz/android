package mega.privacy.android.feature.myaccount.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

/**
 * My Account feature destination.
 *
 * Registers the achievements info and referral bonus screens. The achievements overview screen is
 * still served by the legacy `:app` shim under `AchievementNavKey`; its migrated view is dormant
 * until that shim is flipped to this destination.
 */
class MyAccountFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit =
        { _, _ ->
            achievementsInfo()
            referralBonus()
            megaVpnFreeTrial()
            megaPassFreeTrial()
        }
}
