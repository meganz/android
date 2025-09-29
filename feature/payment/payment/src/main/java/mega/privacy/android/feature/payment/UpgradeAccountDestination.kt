package mega.privacy.android.feature.payment

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountActivity
import mega.privacy.android.navigation.payment.UpgradeAccountSource

@Serializable
data class UpgradeAccountNavKey(
    val source: UpgradeAccountSource = UpgradeAccountSource.UNKNOWN,
) : NavKey

fun NavGraphBuilder.upgradeAccount(removeDestination: () -> Unit) {
    composable<UpgradeAccountNavKey> {
        val context = LocalContext.current
        val args = it.toRoute<UpgradeAccountNavKey>()
        LaunchedEffect(Unit) {
            ChooseAccountActivity.navigateToUpgradeAccount(
                context = context,
                source = args.source
            )

            // Immediately pop this destination from the back stack
            removeDestination()
        }

    }
}
