package mega.privacy.android.app.presentation.contactinfo.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.destination.ContactInfoNavKey

fun NavGraphBuilder.contactInfoLegacyDestination(
    removeDestination: () -> Unit,
) {
    composable<ContactInfoNavKey> {
        val contactInfo = it.toRoute<ContactInfoNavKey>()

        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(
                context,
                ContactInfoActivity::class.java
            )
            intent.putExtra(Constants.NAME, contactInfo.email)
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }

    }
}