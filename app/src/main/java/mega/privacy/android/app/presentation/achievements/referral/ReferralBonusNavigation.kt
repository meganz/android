package mega.privacy.android.app.presentation.achievements.referral

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.achievements.referral.view.ReferralBonusRoute

/**
 * Route for [ReferralBonusRoute]
 */
@Serializable
data object ReferralBonus

/**
 * Composable destination for [ReferralBonusRoute]
 */
fun NavGraphBuilder.referralBonusScreen() {
    composable<ReferralBonus> {
        ReferralBonusRoute()
    }
}

/**
 * Navigation for [ReferralBonusRoute]
 */
fun NavController.navigateToReferralBonus(navOptions: NavOptions? = null) {
    this.navigate(ReferralBonus, navOptions = navOptions)
}