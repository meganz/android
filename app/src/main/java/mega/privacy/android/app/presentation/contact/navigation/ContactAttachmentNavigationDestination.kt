package mega.privacy.android.app.presentation.contact.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ContactAttachmentNavKey


fun EntryProviderScope<NavKey>.contactAttachmentLegacyDestination(removeDestination: (NavKey) -> Unit) {
    entry<ContactAttachmentNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, ContactAttachmentActivity::class.java).apply {
                putExtra(Constants.CHAT_ID, key.chatId)
                putExtra(Constants.MESSAGE_ID, key.messageId)
            }
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination(key)
        }
    }
}

