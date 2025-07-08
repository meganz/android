package mega.privacy.android.app.presentation.achievements.freetrial

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.achievements.freetrial.view.MegaVPNFreeTrialScreen
import mega.privacy.android.app.utils.Constants.MEGA_VPN_PACKAGE_NAME

@Serializable
data class MegaVPNFreeTrial(
    val isReceivedAward: Boolean,
    val storageAmount: Long,
    val awardStorageAmount: Long,
)

/**
 * Route for [MegaVPNFreeTrial]
 */
fun NavGraphBuilder.megaVPNFreeTrialScreen() {
    composable<MegaVPNFreeTrial> { backStackEntry ->
        val context = LocalContext.current
        val megaVPNFreeTrial = backStackEntry.toRoute<MegaVPNFreeTrial>()
        MegaVPNFreeTrialScreen(
            isReceivedAward = megaVPNFreeTrial.isReceivedAward,
            storageAmount = megaVPNFreeTrial.storageAmount,
            awardStorageAmount = megaVPNFreeTrial.awardStorageAmount,
            onInstallButtonClicked = {
                openInSpecificApp(context, MEGA_VPN_PACKAGE_NAME)
            }
        )
    }
}

/**
 * Navigation for [MegaVPNFreeTrial]
 */
fun NavController.navigateToMegaVPNFreeTrial(
    isReceivedAward: Boolean,
    storageAmount: Long,
    awardStorageAmount: Long,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = MegaVPNFreeTrial(isReceivedAward, storageAmount, awardStorageAmount),
        navOptions = navOptions
    )
}