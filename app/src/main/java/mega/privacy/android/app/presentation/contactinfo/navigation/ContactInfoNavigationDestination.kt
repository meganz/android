package mega.privacy.android.app.presentation.contactinfo.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactInfoNavKey

fun EntryProviderScope<NavKey>.contactInfoLegacyDestination(
    removeDestination: () -> Unit,
) {
    entry<ContactInfoNavKey>(
        metadata = transparentMetadata()
    ) { contactInfo ->

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