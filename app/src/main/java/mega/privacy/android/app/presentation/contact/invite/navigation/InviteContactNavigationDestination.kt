package mega.privacy.android.app.presentation.contact.invite.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.InviteContactNavKey

/**
 * Navigation destination for InviteContactActivity that handles legacy navigation.
 * 
 * @param isFromAchievement Whether the entry point is from the achievements screen.
 */
fun EntryProviderScope<NavKey>.inviteContactLegacyDestination(removeDestination: () -> Unit) {
    entry<InviteContactNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, InviteContactActivity::class.java).apply {
                putExtra(InviteContactViewModel.KEY_FROM, key.isFromAchievement)
            }
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

