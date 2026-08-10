package mega.privacy.android.app.presentation.changepassword.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ParkAccountNavKey

/**
 * Launches [ChangePasswordActivity] in park-account mode for a park-account link.
 */
fun EntryProviderScope<NavKey>.parkAccountDestination(removeDestination: () -> Unit) {
    entry<ParkAccountNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current

        LaunchedEffect(key) {
            val intent = Intent(context, ChangePasswordActivity::class.java).apply {
                action = Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT
                data = key.link.toUri()
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}
