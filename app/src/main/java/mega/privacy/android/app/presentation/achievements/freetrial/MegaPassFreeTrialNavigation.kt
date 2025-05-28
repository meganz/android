package mega.privacy.android.app.presentation.achievements.freetrial

import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.achievements.freetrial.view.MegaPassFreeTrialScreen

@Serializable
data class MegaPassFreeTrial(
    val isReceivedAward: Boolean,
)

/**
 * Route for [MegaPassFreeTrial]
 */
fun NavGraphBuilder.megaPassFreeTrialScreen() {
    composable<MegaPassFreeTrial> { backStackEntry ->
        val context = LocalContext.current
        val megaPassFreeTrial = backStackEntry.toRoute<MegaPassFreeTrial>()
        MegaPassFreeTrialScreen(
            isReceivedAward = megaPassFreeTrial.isReceivedAward,
            onInstallButtonClicked = {
                openInSpecificApp(context, MEGA_PASS_PACKAGE_NAME)
            }
        )
    }
}

/**
 * Navigation for [MegaPassFreeTrial]
 */
fun NavController.navigateToMegaPassFreeTrial(
    isReceivedAward: Boolean,
    navOptions: NavOptions? = null,
) {
    this.navigate(route = MegaPassFreeTrial(isReceivedAward), navOptions = navOptions)
}

internal fun openInSpecificApp(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "market://details?id=$packageName".toUri()
            )
        )
    }.onFailure {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
        )
    }
}

private const val MEGA_PASS_PACKAGE_NAME = "mega.pwm.android.app"