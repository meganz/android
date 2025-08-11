package mega.privacy.android.app.activities.destinations

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarning

fun NavGraphBuilder.overDiskQuotaPaywallWarning(removeDestination: () -> Unit) {
    composable<OverDiskQuotaPaywallWarning> {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(
                context,
                OverDiskQuotaPaywallActivity::class.java
            )
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }

    }
}