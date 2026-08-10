package mega.privacy.android.app

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.BusinessExpiredAlertNavKey

/**
 * Navigation destination for BusinessExpiredAlertActivity that handles legacy navigation.
 *
 * Note: This navigates to the Activity only if no compose implementation exists.
 * If a compose implementation is available, it should be used instead.
 */
fun EntryProviderScope<NavKey>.businessExpiredAlertLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<BusinessExpiredAlertNavKey>(
        metadata = transparentMetadata()
    ) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, BusinessExpiredAlertActivity::class.java)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}
