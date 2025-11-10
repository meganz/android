package mega.privacy.android.app.presentation.chat.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatsNavKey

fun EntryProviderScope<NavKey>.chatListLegacyDestination(removeDestination: () -> Unit) {
    entry<ChatsNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, ChatHostActivity::class.java).apply {
                putExtra(ChatHostActivity.OPEN_CHAT_LIST, true)
            }
            context.startActivity(intent)
            removeDestination()
        }
    }
}
