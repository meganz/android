package mega.privacy.android.app.presentation.achievements.referral

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.achievements.referral.view.ReferralBonusRoute

/**
 * Route for [ReferralBonusRoute]
 */
internal const val referralBonusRoute = "achievements/referrals"

/**
 * Composable destination for [ReferralBonusRoute]
 */
fun NavGraphBuilder.referralBonusScreen() {
    composable(route = referralBonusRoute) {
        ReferralBonusRoute()
    }
}

/**
 * Navigation for [ReferralBonusRoute]
 */
fun NavController.navigateToReferralBonus(navOptions: NavOptions? = null) {
    this.navigate(route = referralBonusRoute, navOptions = navOptions)
}